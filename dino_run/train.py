import random
import itertools
import numpy as np
import os 
import logging

import torch
import torch.nn.functional as F
import torch.optim as optim
from model import DQN
from memory import Transition, ReplayMemory

import gym 
import gym_chrome_dino
from gym_chrome_dino.utils.wrappers import make_dino

def ep_decay(start_val, end_val, steps): 
    decay_rate = (start_val - end_val) / steps
    if start_val > end_val: 
        start_val -= decay_rate
    return start_val 

def get_state(state): 
    # convert state to numpy arr then tensor     
    state = np.array(state).transpose((2, 0, 1))
    state = np.ascontiguousarray(state, dtype=np.float32) / 255
    state = torch.from_numpy(state)
    return state.unsqueeze(0) 


def save(file_name):
    cp_file = 'checkpoints/'
    if os.path.exists(cp_file)==False: 
        os.makedirs(cp_file)
    # save model
    torch.save(policy_net.state_dict(), cp_file + file_name)

def choose_action(state): 
    # implement epsilon-greedy algorithm 
    global EPS_INIT
    sample = random.random()
    eps = ep_decay(EPS_INIT, EPS_END, EXPLORE_STEP) 
    EPS_INIT = eps
    if sample > eps: 
        with torch.no_grad():
            return policy_net(state).max(1)[1].view(1, 1), eps
    else:
        action = random.randrange(n_actions)
        return torch.tensor([[action]], device=device, dtype=torch.long), eps
    

def expected_q(next_states, rewards):
    # count expected q value, use to calculate loss 
    # only use those next state is not the end of the game
    # bellman equation: q = r + gamma * q_next
    non_final_mask = torch.tensor(
        tuple(map(lambda s: s is not None, next_states)),
        device=device, dtype=torch.bool)
    non_final_next_states = torch.cat([s for s in next_states if s is not None])

    # put the state into the network and filter those action with the max q value
    q_next = torch.zeros(BATCH_SIZE, device=device)
    q_next[non_final_mask] = target_net(non_final_next_states).max(1)[0].detach()
    expected_q = rewards + GAMMA * q_next

    return expected_q.unsqueeze(1)
    
    
def optimize():
    if len(memory) < BATCH_SIZE:
        return
    transitions = memory.sample(BATCH_SIZE)
    batch = Transition(*zip(*transitions))
    states = torch.cat(batch.state)
    actions = torch.cat(batch.action)
    rewards = torch.cat(batch.reward)
    actual_q = policy_net(states).gather(1, actions)
    expected_q_value = expected_q(batch.next_state, rewards)
    loss = F.smooth_l1_loss(actual_q, expected_q_value) # loss between actual q and expected q

    # optimize the model
    optimizer.zero_grad()
    loss.backward()
    for param in policy_net.parameters():
        param.grad.data.clamp_(-1, 1)
    optimizer.step()
        
def train(env, n_episodes, logger):
    optim_count = 0     
    for ep in range(n_episodes): 
        total_reward = 0 
        state = get_state(env.reset()).to(device)
        for t in itertools.count(): 
            action, epsilon = choose_action(state)
            
            next_state, reward, done, _ = env.step(action)
            total_reward += reward 
            reward = torch.tensor([reward], dtype=torch.float32, device=device)
            if done:
                memory.push(state, action, None, reward)
                optimize()
                break
            else:
                next_state = get_state(next_state).to(device)
                memory.push(state, action, next_state, reward)
                optimize()
                
            state = next_state
            
        optim_count += t
        score = env.unwrapped.game.get_score()
        logger.info(f"{ep},{optim_count},{total_reward:.1f},{score},{epsilon:.6f}")

        if ep % TARGET_UPDATE == 0:
            target_net.load_state_dict(policy_net.state_dict())
            save(f"model_{ep}.pth")
      
    
def test(self, env):
    while True:
        state = get_state(env.reset()).to(device)
        while True:
            with torch.no_grad():
                action = policy_net(state).max(1)[1].view(1, 1)
            next_state, _, done, _ = env.step(action)

            if done:
                break
            next_state = get_stater(next_state).to(device)
            state = next_state

            
if __name__ == '__main__': 
    # enable cuda is available 
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    
    # set seed 
    torch.manual_seed(12)
    
    # hyperparameters 
    MEMORY_SIZE = 30000
    BATCH_SIZE = 128
    GAMMA = 0.99
    EPS_INIT = 1e-1
    EPS_END = 1e-4
    EXPLORE_STEP = 1e5
    LR = 2e-5
    TARGET_UPDATE = 5
    N_EPISODE = 2001
                           
    # Initialise the game
    env = gym.make('ChromeDino-v0')
    # env = gym.make('ChromeDinoNoBrowser-v0')
    env = make_dino(env, timer=True, frame_stack=True)
    # Get the number of actions and the dimension of input
    n_actions = env.action_space.n
    
    memory = ReplayMemory(MEMORY_SIZE)
    # initialise networks 
    policy_net = DQN(n_actions=n_actions).to(device)
    
    # load pretrained mode -- transfer learning     
#     pretrained_model = torch.load('trained/dqn.pkl')
#     pretrained_model['fc4.weight'] = pretrained_model.pop('fc.weight')
#     pretrained_model['fc4.bias'] = pretrained_model.pop('fc.bias')
#     policy_net.load_state_dict(pretrained_model)
    
    
    target_net = DQN(n_actions=n_actions).to(device)
    target_net.load_state_dict(policy_net.state_dict())
    target_net.eval()
    
    optimizer = optim.RMSprop(policy_net.parameters(), lr=LR)
    
    # Setup logging
    formatter = logging.Formatter(r'"%(asctime)s",%(message)s')
    logger = logging.getLogger("dino-rl")
    logger.setLevel(logging.INFO)
    # save logging into .csv file 
    fh = logging.FileHandler("./dino-log.csv")
    fh.setFormatter(formatter)
    logger.addHandler(fh) 
    
    train(env, N_EPISODE, logger)