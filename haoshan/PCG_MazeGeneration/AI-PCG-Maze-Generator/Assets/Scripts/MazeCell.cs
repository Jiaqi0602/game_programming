using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class MazeCell
{
    public bool Visited = false;
    public GameObject Upwall;
    public GameObject Downwall;
    public GameObject Leftwall;
    public GameObject Rightwall;
    public bool up = true;
    public bool down = true;
    public bool left = true;
    public bool right = true;
}
