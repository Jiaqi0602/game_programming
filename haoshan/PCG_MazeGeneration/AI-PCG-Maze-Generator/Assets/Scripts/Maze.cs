using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class Maze : MonoBehaviour
{
    public int Rows = 20;
    public int Columns = 20;
    public GameObject Wall;
    public GameObject Floor;
    public InputField HeightField;
    public InputField WidthField;

    private MazeCell[,] grid;
    private int currentRow = 0;
    private int currentColumn = 0;
    private bool scanComplete = false;

    // For Complexity Analysis
    private int dead_ends_counts = 0;
    private int juntions_counts = 0;
    private int crossroads_counts = 0;
    private int turns_counts = 0;
    private int straightways_counts = 0;
    private int total_counts = 0;

    // Start is called before the first frame update
    void Start()
    {
        // Normal maze generation
        GenerateGrid(1);

        // Get the average of maze complexity with multiple runs
        //GenerateGrid(100);
    }



    void GenerateGrid(int times)
    {
        if (times == 1)
        {
            // destroy all the children of this transform object
            foreach (Transform transform in transform)
            {
                Destroy(transform.gameObject);
            }

            // Create the grid with all the walls and floors
            // dimensions should be at least 2x2
            CreateGrid();

            ChangeCameraPosition();

            currentRow = 0;
            currentColumn = 0;
            scanComplete = false;  // initialise

            // Run the algorithm to carve the path from top left to bottom right
            HuntAndKill();

            // For Complexity Analysis
            Evaluator();

        }else 
        {
            while(times > 0)
            {
                foreach (Transform transform in transform)
                {
                    Destroy(transform.gameObject);
                }

                CreateGrid();

                ChangeCameraPosition();

                currentRow = 0;
                currentColumn = 0;
                scanComplete = false;  

                HuntAndKill();

                object[] args = Evaluator();

                total_counts += (int)args[0];
                dead_ends_counts += (int)args[1];
                straightways_counts += (int)args[2];
                turns_counts += (int)args[3];
                juntions_counts += (int)args[4];
                crossroads_counts += (int)args[5];

                times--;
            }

            float dead_ends_percent = (dead_ends_counts * 1.0f / total_counts) * 100;
            float straightways_percent = (straightways_counts * 1.0f / total_counts) * 100;
            float turns_percent = (turns_counts * 1.0f / total_counts) * 100;
            float junctions_percent = (juntions_counts * 1.0f / total_counts) * 100;
            float crossroads_percent = (crossroads_counts * 1.0f / total_counts) * 100;

            object[] avg_args = new object[] { total_counts,
            dead_ends_counts, straightways_counts,
            turns_counts, juntions_counts, crossroads_counts,
            dead_ends_percent, straightways_percent,
            turns_percent, junctions_percent, crossroads_percent };

            ShowResult(avg_args);
        }
    }

    object[] Evaluator()
    {
         int dead_ends_count = 0;
         int juntions_count = 0;
         int crossroads_count = 0;
         int turns_count = 0;
         int straightways_count = 0;
         int total_count = Rows * Columns;

        for (int i = 0; i < Rows; i++)
        {
            for (int j = 0; j < Columns; j++)
            {
                if (IsDeadEnd(i, j)) dead_ends_count++;
                else if (IsStraightway(i, j)) straightways_count++;
                else if (IsTurns(i, j)) turns_count++;
                else if (IsTJunctions(i, j)) juntions_count++;
                else crossroads_count++;
            }
        }

        float dead_ends_percent = (dead_ends_count * 1.0f / total_count) * 100;
        float straightways_percent = (straightways_count * 1.0f / total_count) * 100;
        float turns_percent = (turns_count * 1.0f / total_count) * 100;
        float junctions_percent = (juntions_count * 1.0f / total_count) * 100;
        float crossroads_percent = (crossroads_count * 1.0f / total_count) * 100;

        object[] args = new object[] { total_count,
            dead_ends_count, straightways_count,
            turns_count, juntions_count, crossroads_count,
            dead_ends_percent, straightways_percent,
            turns_percent, junctions_percent, crossroads_percent };

        ShowResult(args);

        return args;
        
    }

    void ShowResult(object[] args)
    {
        string msg = "Complexity Analysis==================" +
            "\n\tTotal Numbers of Grid : {0}" +
            "\n\tNumbers of Dead Ends : {1}" +
            "\n\tNumbers of Straightways : {2}" +
            "\n\tNumbers of Turns : {3}" +
            "\n\tNumbers of T-junctions : {4}" +
            "\n\tNumbers of Crossroads : {5}" +
            "\nPercentage======================" +
            "\n\tDead Ends : {6:0.00}%" +
            "\n\tStraightways : {7:0.00}%" +
            "\n\tTurns : {8:0.00}%" +
            "\n\tT-junctions : {9:0.00}%" +
            "\n\tCrossroads : {10:0.00}%" +
            "\n==============================";

        print(string.Format(msg, args));
    }

    bool IsDeadEnd(int i, int j) // has only one entrance/exit
    {
        if (grid[i, j].left
            && grid[i, j].right
            && grid[i, j].down && !grid[i, j].up)
            return true;
        else if (grid[i, j].left
            && grid[i, j].up
            && grid[i, j].down && !grid[i, j].right)
            return true;
        else if (grid[i, j].left
            && grid[i, j].up
            && grid[i, j].right && !grid[i, j].down)
            return true;
        else if (grid[i, j].right
            && grid[i, j].up
            && grid[i, j].down && !grid[i, j].left)
            return true;
        else
            return false;
    }

    bool IsStraightway(int i, int j) // has opposite entrance/exit
    {
        if (grid[i, j].left
            && grid[i, j].right && !grid[i, j].up && !grid[i, j].down)
            return true;
        else if (grid[i, j].up
            && grid[i, j].down && !grid[i, j].left && !grid[i, j].right)
            return true;
        else
            return false;
    }

    bool IsTurns(int i, int j) // has two entrance/exit
    {
        if (grid[i, j].left
            && grid[i, j].down)
            return true;
        else if (grid[i, j].left
            && grid[i, j].up)
            return true;
        else if (grid[i, j].up
            && grid[i, j].right)
            return true;
        else if (grid[i, j].right
            && grid[i, j].down)
            return true;
        else
            return false;
    }

    bool IsTJunctions(int i, int j) // has three entrance/exit
    {
        if (grid[i, j].left)
            return true;
        else if (grid[i, j].up)
            return true;
        else if (grid[i, j].right)
            return true;
        else if (grid[i, j].down)
            return true;
        else
            return false;
    }

    void CreateGrid()
    {
        float size = Wall.transform.localScale.x;
        grid = new MazeCell[Rows, Columns];

        for (int i = 0; i < Rows; i++)
        {
            for (int j = 0; j < Columns; j++)
            {
                GameObject floor = Instantiate(Floor, new Vector3(j * size, 0, -i * size), Quaternion.identity);
                //floor.name = "Floor_" + i + "_" + j;

                GameObject upWall = Instantiate(Wall, new Vector3(j * size, 1.75f, -i * size + 1.25f), Quaternion.identity);
                //upWall.name = "UpWall_" + i + "_" + j;

                GameObject downWall = Instantiate(Wall, new Vector3(j * size, 1.75f, -i * size - 1.25f), Quaternion.identity);
                //downWall.name = "DownWall_" + i + "_" + j;

                GameObject leftWall = Instantiate(Wall, new Vector3(j * size - 1.25f, 1.75f, -i * size), Quaternion.Euler(0, 90, 0));
                //leftWall.name = "LeftWall_" + i + "_" + j;

                GameObject rightWall = Instantiate(Wall, new Vector3(j * size + 1.25f, 1.75f, -i * size), Quaternion.Euler(0, 90, 0));
                //rightWall.name = "RightWall_" + i + "_" + j;

                grid[i, j] = new MazeCell();
                grid[i, j].Upwall = upWall;
                grid[i, j].Downwall = downWall;
                grid[i, j].Leftwall = leftWall;
                grid[i, j].Rightwall = rightWall;

                floor.transform.parent = transform;
                upWall.transform.parent = transform;
                downWall.transform.parent = transform;
                leftWall.transform.parent = transform;
                rightWall.transform.parent = transform;

                grid[i, j].left = true;
                grid[i, j].right = true;
                grid[i, j].up = true;
                grid[i, j].down = true;


                // destroy top left wall to create entrance
                if (i==0 && j == 0)
                {
                    Destroy(leftWall);
                    grid[i, j].left = false;
                    /*
                    Debug.Log("Left Right Up Down Wall ==============");
                    Debug.Log(grid[i, j].left);
                    Debug.Log(grid[i, j].right);
                    Debug.Log(grid[i, j].up);
                    Debug.Log(grid[i, j].down);
                    */

                }
                // destroy bottom right wall to create exit
                if (i == Rows-1 && j == Columns-1)
                {
                    Destroy(rightWall);
                    grid[i, j].right = false;
                }
            }
        }
    }

    void ChangeCameraPosition()
    {
        float size = Wall.transform.localScale.x;
        Vector3 cameraPosition = Camera.main.transform.position;
        cameraPosition.x = Mathf.Round(Columns/2) * size;
        cameraPosition.y = Mathf.Max(13, Mathf.Max(Rows, Columns) * 3.5f);
        cameraPosition.z = (-Mathf.Round(Rows/ 2) * size);
        Camera.main.transform.position = cameraPosition;
    }

    // Update is called once per frame
    void HuntAndKill()
    {
        // mark the first cell of the random walk as visited
        grid[currentRow, currentColumn].Visited = true;

        while(!scanComplete)
        {
            Walk();
            Hunt();
        }
        
    }

    void Walk()
    {
        while (AreThereUnvisitedNeighbors())
        {
            // go to a random direction
            int direction = Random.Range(0, 4);

            if (direction == 0) //check up
            {
                if (IsCellUnVisitedAndInBoundary(currentRow - 1, currentColumn))
                {
                    if (grid[currentRow, currentColumn].Upwall)
                    {
                        Destroy(grid[currentRow, currentColumn].Upwall);
                        grid[currentRow, currentColumn].up = false;
                    }

                    currentRow--;
                    grid[currentRow, currentColumn].Visited = true;

                    if (grid[currentRow, currentColumn].Downwall)
                    {
                        Destroy(grid[currentRow, currentColumn].Downwall);
                        grid[currentRow, currentColumn].down = false;
                    }
                }
            }
            else if (direction == 1) //check down
            {
                if (IsCellUnVisitedAndInBoundary(currentRow + 1, currentColumn))
                {
                    if (grid[currentRow, currentColumn].Downwall)
                    {
                        Destroy(grid[currentRow, currentColumn].Downwall);
                        grid[currentRow, currentColumn].down = false;
                    }

                    currentRow++;
                    grid[currentRow, currentColumn].Visited = true;

                    if (grid[currentRow, currentColumn].Upwall)
                    {
                        Destroy(grid[currentRow, currentColumn].Upwall);
                        grid[currentRow, currentColumn].up = false;
                    }
                }
            }
            else if (direction == 2)  //check left
            {
                if (IsCellUnVisitedAndInBoundary(currentRow, currentColumn - 1))
                {
                    if (grid[currentRow, currentColumn].Leftwall)
                    {
                        Destroy(grid[currentRow, currentColumn].Leftwall);
                        grid[currentRow, currentColumn].left = false;
                    }

                    currentColumn--;
                    grid[currentRow, currentColumn].Visited = true;

                    if (grid[currentRow, currentColumn].Rightwall)
                    {
                        Destroy(grid[currentRow, currentColumn].Rightwall);
                        grid[currentRow, currentColumn].right = false;
                    }
                }
            }
            else if (direction == 3)  //check right
            {
                if (IsCellUnVisitedAndInBoundary(currentRow, currentColumn + 1))
                {
                    if (grid[currentRow, currentColumn].Rightwall)
                    {
                        Destroy(grid[currentRow, currentColumn].Rightwall);
                        grid[currentRow, currentColumn].right = false;
                    }

                    currentColumn++;
                    grid[currentRow, currentColumn].Visited = true;

                    if (grid[currentRow, currentColumn].Leftwall)
                    {
                        Destroy(grid[currentRow, currentColumn].Leftwall);
                        grid[currentRow, currentColumn].left = false;
                    }
                }
            }
        }
    }

    void Hunt()
    {
        scanComplete = true;
        for(int i=0; i < Rows; i++)
        {
            for(int j=0; j < Columns; j++)
            {
                if(!grid[i, j].Visited && AreThereVisitedNeighbors(i, j))
                {
                    scanComplete = false;
                    currentRow = i; 
                    currentColumn = j;
                    grid[currentRow, currentColumn].Visited = true;
                    DestroyAdjacentWall();
                    return;
                }
            }
        }
    }

    void DestroyAdjacentWall()
    {
        bool destroyed = false;
        
        while (!destroyed)
        {
            int direction = Random.Range(0, 4);
            if (direction == 0) //check up
            {
                if(currentRow > 0 && grid[currentRow - 1, currentColumn].Visited)
                {
                    if (grid[currentRow - 1, currentColumn].Downwall)
                    {
                        Destroy(grid[currentRow - 1, currentColumn].Downwall);
                        grid[currentRow - 1, currentColumn].down = false;
                    }
                    if (grid[currentRow, currentColumn].Upwall)
                    {
                        Destroy(grid[currentRow, currentColumn].Upwall);
                        grid[currentRow, currentColumn].up = false;
                    }
                    destroyed = true;
                }
            }
            else if (direction == 1) //check down
            {
                if (currentRow < Rows - 1 && grid[currentRow + 1, currentColumn].Visited)
                {
                    if (grid[currentRow + 1, currentColumn].Upwall)
                    {
                        Destroy(grid[currentRow + 1, currentColumn].Upwall);
                        grid[currentRow + 1, currentColumn].up = false;
                    }
                    if (grid[currentRow, currentColumn].Downwall)
                    {
                        Destroy(grid[currentRow, currentColumn].Downwall);
                        grid[currentRow, currentColumn].down = false;
                    }
                    destroyed = true;
                }
            }
            else if (direction == 2) //check left
            {
                if (currentColumn > 0 && grid[currentRow, currentColumn - 1].Visited)
                {
                    if (grid[currentRow, currentColumn - 1].Rightwall)
                    {
                        Destroy(grid[currentRow, currentColumn - 1].Rightwall);
                        grid[currentRow, currentColumn - 1].right = false;
                    }
                    if (grid[currentRow, currentColumn].Leftwall)
                    {
                        Destroy(grid[currentRow, currentColumn].Leftwall);
                        grid[currentRow, currentColumn].left = false;
                    }
                    destroyed = true;
                }
            }
            else if (direction == 3) //check right
            {
                if (currentColumn < Columns - 1 && grid[currentRow, currentColumn + 1].Visited)
                {
                    if (grid[currentRow, currentColumn + 1].Leftwall)
                    {
                        Destroy(grid[currentRow, currentColumn + 1].Leftwall);
                        grid[currentRow, currentColumn + 1].left = false;
                    }
                    if (grid[currentRow, currentColumn].Rightwall)
                    {
                        Destroy(grid[currentRow, currentColumn].Rightwall);
                        grid[currentRow, currentColumn].right = false;
                    }
                    destroyed = true;
                }
            }
        }
        
    }

    bool AreThereUnvisitedNeighbors()
    {
        if (IsCellUnVisitedAndInBoundary(currentRow - 1, currentColumn)) //check up
        {
            return true;
        }
        if (IsCellUnVisitedAndInBoundary(currentRow + 1, currentColumn)) //check down
        {
            return true;
        }
        if (IsCellUnVisitedAndInBoundary(currentRow, currentColumn - 1)) //check left
        {
            return true;
        }
        if (IsCellUnVisitedAndInBoundary(currentRow, currentColumn + 1)) //check right
        {
            return true;
        }
        return false;
    }

    public bool AreThereVisitedNeighbors(int row, int column)
    {
        if (row > 0 && grid[row - 1, column].Visited) //check up
        {
            return true;
        }

        if (row < Rows - 1 && grid[row + 1, column].Visited) //check down
        {
            return true;
        }

        if (column > 0 && grid[row, column - 1].Visited) //check left
        {
            return true;
        }

        if (column < Columns - 1 && grid[row, column + 1].Visited) //check right
        {
            return true;
        }
        return false;
    }

    bool IsCellUnVisitedAndInBoundary(int row, int column)
    {
        // do boundary check and visited check
        if (row >= 0 && row < Rows && column >= 0 && column < Columns
            && !grid[row, column].Visited)
        {
            return true;
        }
        return false;
    }

    public void Regenerate()
    {
        int rows = 2;
        int columns = 2;

        if(int.TryParse(HeightField.text, out rows))
        {
            Rows = Mathf.Max(2, rows);
        }
        if (int.TryParse(WidthField.text, out columns))
        {
            Columns = Mathf.Max(2, columns);
        }

        GenerateGrid(1);
    }
}
