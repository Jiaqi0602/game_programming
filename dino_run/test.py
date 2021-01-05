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

def get_state(state): 
    # convert state to numpy arr then tensor     
    state = np.array(state).transpose((2, 0, 1))
    state = np.ascontiguousarray(state, dtype=np.float32) / 255
    state = torch.from_numpy(state)
    return state.unsqueeze(0) 

    
def test(env):
    while True:
        state = get_state(env.reset()).to(device)
        while True:
            with torch.no_grad():
                action = policy_net(state).max(1)[1].view(1, 1)
            next_state, _, done, _ = env.step(action)

            if done:
                break
            next_state = get_state(next_state).to(device)
            state = next_state

            
if __name__ == '__main__': 
    # enable cuda is available 
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    
    # Initialise the game
    env = gym.make('ChromeDino-v0')
    # env = gym.make('ChromeDinoNoBrowser-v0')
    env = make_dino(env, timer=True, frame_stack=True)
    # Get the number of actions and the dimension of input
    n_actions = env.action_space.n
    
    # initialise networks 
    policy_net = DQN(n_actions=n_actions).to(device)
    trained_model = torch.load('checkpoints/model_2000.pth')
    policy_net.load_state_dict(trained_model)
    policy_net.eval() 
    
    test(env)