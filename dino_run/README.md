# DQN-Based Dino Run 

Dino run is a hidden endless runner game in Chrome offline mode. The dinosaur can be controlled by space/up and down arrow keys to jump and duck to avoid the obstacles in the game. 

In this mini project, we developed a Deep Q-Networks (DQN) agent to play this game.  

Game environment used is based on [gym_chrome_dino](https://github.com/elvisyjlin/gym-chrome-dino), implementation of the Deep Q-learning algorithm is inspired by this [repo](https://github.com/jmichaux/dqn-pytorch) and this [paper](http://cs229.stanford.edu/proj2016/report/KeZhaoWei-AIForChromeOfflineDinosaurGame-report.pdf). 

The weights of our trained model are included in the ```checkpoints``` folder. 

## Installation

1. Install: 

```bash
# With pip: 
pip install gym
pip install selenium
pip install pytorch 
pip install opencv-python
```
2. Download ChromeDriver from [here](https://chromedriver.chromium.org/) and move ```chromedriver.exe``` to  ```/gym_chrome_dino``` folder. 

3. Run the code 

To start the training process: 
```bash 
python train.py
```

To test the trained model: 
```bash
python test.py 
```


## Results & Evaluation 