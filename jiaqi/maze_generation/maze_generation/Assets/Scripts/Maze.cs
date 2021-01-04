using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class Maze : MonoBehaviour
{
    public int Rows = 15;
    public int Columns = 15 ; 
    public GameObject Wall;
    public GameObject Floor;

    private MazeCell[,] grid;

    // starting location [0,0] 
    private int currentRow = 0;
    private int currentColumn = 0;
    private bool mazeComplete = false; 

    // Start is called before the first frame update
    void Start()
    {
        // create walls and floors 
        createGrid();      
        // algorithm to generate the maze 
        huntAndKill(); 
        
    }

    void createGrid()
    {
        float size = Wall.transform.localScale.x;
        grid = new MazeCell[Rows, Columns];

        for (int i = 0; i < Rows; i++)
        {
            for (int j = 0; j < Columns; j++)
            {

                grid[i, j] = new MazeCell();
                grid[i, j] = new MazeCell();

                GameObject newFloor = (GameObject)Instantiate(Floor, new Vector3(j * size,0, -i * size), Quaternion.identity);
                newFloor.name = "floor_" + i + "_" + j;

                GameObject upWall = (GameObject)Instantiate(Wall, new Vector3(j * size , 1.75f , -i * size + 1.25f), Quaternion.identity);
                upWall.name = "Upwall_" + i + "_" + j;

                GameObject downWall = (GameObject)Instantiate(Wall, new Vector3(j * size , 1.75f , -i * size - 1.25f ), Quaternion.identity);
                downWall.name = "Downwall_" + i + "_" + j;

                GameObject leftWall = (GameObject)Instantiate(Wall, new Vector3(j * size - 1.25f , 1.75f, -i * size ), Quaternion.Euler(0, 90, 0));
                leftWall.name = "leftWall_" + i + "_" + j;

                GameObject rightWall = (GameObject)Instantiate(Wall, new Vector3(j * size + 1.25f, 1.75f, -i * size), Quaternion.Euler(0, 90, 0));
                rightWall.name = "rightWall_" + i + "_" + j;

                grid[i, j].upWall = upWall;
                grid[i, j].downWall = downWall;
                grid[i, j].leftWall = leftWall;
                grid[i, j].rightWall = rightWall;

                newFloor.transform.parent = transform;
                upWall.transform.parent = transform;
                downWall.transform.parent = transform;
                leftWall.transform.parent = transform;
                rightWall.transform.parent = transform;

                // make an entrance
                if(i==0 && j == 0)
                {
                    Destroy(leftWall); 
                }

                if(i==Rows-1 && j == Columns - 1)
                {
                    Destroy(rightWall); 
                }
            }
        }

    }


    void huntAndKill()
    {
        grid[currentRow, currentColumn].Visited = true;
        while (!mazeComplete)
        {
            Walk();
            Hunt();
        }

    }


    // check unvisited neigbours in every directions 
    bool checkUnvisitedNeighbour()
    {
        if(isNextCellUnvisited(currentRow-1, currentColumn))
        {
            return true; 
        }
        if (isNextCellUnvisited(currentRow + 1, currentColumn))
        {
            return true;
        }
        if (isNextCellUnvisited(currentRow, currentColumn + 1))
        {
            return true;
        }
        if (isNextCellUnvisited(currentRow, currentColumn - 1))
        {
            return true;
        }
        return false;  
    }

    // check is the unvisit neighbour and within maze boundaries 
    bool isNextCellUnvisited(int row, int column)
    {
        if (row>=0 && row < Rows && column>=0 && column < Columns && !grid[row, column].Visited)
        {
            return true; 
        }
        return false; 
    }

    public bool checkVisitedNeighbour(int row, int col)
    {
        if (row > 0 && grid[row-1, col].Visited)
        {
            return true; 
        }
        if (row < Rows -1 && grid[row + 1, col].Visited)
        {
            return true;
        }
        if (col > 0 && grid[row, col - 1].Visited)
        {
            return true;
        }
        if (col < Columns -1 && grid[row, col + 1].Visited)
        {
            return true;
        }

        return false; 
    }


    bool isNextCellVisited(int row, int column)
    {
        if (row >= 0 && row < Rows && column >= 0 && column < Columns && grid[row, column].Visited)
        {
            return true;
        }
        return false;
    }

    // first, check all grids is there unvisited neighbour (checkUnvisitedNeighbour) 
    // if there's unvisited neighbour, random walk
    // in random walk step, check is the next grid unvisited (isNextCellUnvisited) 

    void Walk()
    {
        while (checkUnvisitedNeighbour())
        {
            // random direction 
            int direction = Random.Range(0, 4);
            // check available movement 
            // up
            if (direction == 0)
            {
                Debug.Log("check up");

                if (isNextCellUnvisited(currentRow - 1, currentColumn))
                {

                    if (grid[currentRow, currentColumn].upWall)
                    {
                        Destroy(grid[currentRow, currentColumn].upWall);
                    }

                    currentRow--;
                    grid[currentRow, currentColumn].Visited = true;

                    if (grid[currentRow, currentColumn].downWall)
                    {
                        Destroy(grid[currentRow, currentColumn].downWall);
                    }

                }
            }
            // down
            else if (direction == 1)
            {
                Debug.Log("check down");
                // make sure the current row located is not the last row 
                if (isNextCellUnvisited(currentRow + 1, currentColumn))
                {
                    if (grid[currentRow, currentColumn].downWall)
                    {
                        Destroy(grid[currentRow, currentColumn].downWall);
                    }

                    currentRow++;
                    grid[currentRow, currentColumn].Visited = true;

                    if (grid[currentRow, currentColumn].upWall)
                    {
                        Destroy(grid[currentRow, currentColumn].upWall);
                    }
                }

            }
            // left
            else if (direction == 2)
            {
                Debug.Log("check left");

                if (isNextCellUnvisited(currentRow, currentColumn - 1))
                {
                    if (grid[currentRow, currentColumn].leftWall)
                    {
                        Destroy(grid[currentRow, currentColumn].leftWall);
                    }
                    currentColumn--;
                    grid[currentRow, currentColumn].Visited = true;

                    if (grid[currentRow, currentColumn].rightWall)
                    {
                        Destroy(grid[currentRow, currentColumn].rightWall);
                    }
                }

            }
            // right
            else if (direction == 3)
            {
                Debug.Log("check right");

                if (isNextCellUnvisited(currentRow, currentColumn + 1))
                {

                    if (grid[currentRow, currentColumn].rightWall)
                    {
                        Destroy(grid[currentRow, currentColumn].rightWall);
                    }

                    currentColumn++;
                    grid[currentRow, currentColumn].Visited = true;

                    if (grid[currentRow, currentColumn].leftWall)
                    {
                        Destroy(grid[currentRow, currentColumn].leftWall);
                    }
                }

            }
        }

    }

    void Hunt()
    {
        mazeComplete = true; 
        for (int i=0; i<Rows; i++)
        {
            for(int j=0; j<Columns; j++)
            {
                // grid[i, j] is not visited & the neighbour is visited 

                if (!grid[i, j].Visited && checkVisitedNeighbour(i, j))
                {
                    // maze not complete if there's grid not visited
                    mazeComplete = false; 
                    currentRow = i;
                    currentColumn = j;
                    grid[currentRow, currentColumn].Visited = true;
                    // randomly destroy the wall adjacent to visited neighbour 
                    destroyAdjacentWall();
                    return; 
                }
            }
        }

    }


    // destroy adjacent wall next to the visited neighbout 
    // need to make a passage otherwise will be all dead end 
    // randomly choose one wall next to the visited neighbours to destroy 

    // check if the adjacent wall existed 
    void destroyAdjacentWall()
    {
        bool destroyed = false;
        while (!destroyed)
        {
            int direction = Random.Range(0, 4);
            if (direction == 0)
            {
                if (currentRow > 0 && grid[currentRow - 1, currentColumn].Visited)
                {

                    if (grid[currentRow - 1, currentColumn].downWall)
                    {
                        Destroy(grid[currentRow - 1, currentColumn].downWall); 
                    }
                    if (grid[currentRow, currentColumn].upWall)
                    {
                        Destroy(grid[currentRow, currentColumn].upWall);
                    }

                    destroyed = true; 
                }
            
            }
            else if (direction == 1)
            {
                if (currentRow < Rows-1 && grid[currentRow + 1 , currentColumn].Visited)
                {
                    if (grid[currentRow + 1, currentColumn].upWall)
                    {
                        Destroy(grid[currentRow + 1, currentColumn].upWall);
                    }

                    if (grid[currentRow , currentColumn].downWall)
                    {
                        Destroy(grid[currentRow, currentColumn].downWall);
                    }

                    destroyed = true;
                }

            }
            else if (direction == 2)
            {
                if (currentColumn > 0 && grid[currentRow, currentColumn - 1].Visited)
                {
                    if (grid[currentRow, currentColumn - 1].rightWall)
                    {
                    Destroy(grid[currentRow , currentColumn -1 ].rightWall);
                    }

                    if (grid[currentRow, currentColumn].leftWall)
                    {
                        Destroy(grid[currentRow, currentColumn].leftWall);
                    }

                    destroyed = true;
                }

            }
            else if (direction == 3)
            {
                if (currentColumn < Columns-1 && grid[currentRow, currentColumn + 1].Visited)
                {
                    if (grid[currentRow, currentColumn + 1].leftWall)
                    {
                        Destroy(grid[currentRow, currentColumn + 1].leftWall);
                    }

                    if (grid[currentRow, currentColumn].rightWall)
                    {
                        Destroy(grid[currentRow, currentColumn].rightWall);
                    }

                    destroyed = true;
                }
            }
        }


    }
    


    // Update is called once per frame
    void Update()
    {

    }


}
