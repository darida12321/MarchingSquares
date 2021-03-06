import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class MarchingCubes extends PApplet {




InputManager inputManager;
BallManager ballManager;

public void setup(){
  
  //fullScreen();
  frameRate(60);

  inputManager = new InputManager(1);
  ballManager = new BallManager(20, 100);
}

public void draw(){
  ballManager.update(false);
  //inputManager.update();

  //println(frameRate);
}
class Ball{
  PVector pos;
  PVector vel;
  float r;

  Ball(){
    r = random(width/50, width/10);
    float x = random(r, width-r);
    float y = random(r, height-r);
    pos = new PVector(x, y);

    float angle = random(2*PI);
    float speed = random(width/1000, width/500);
    vel = new PVector(cos(angle) * speed, sin(angle) * speed);
  }

  public void update(){
    pos.add(vel);
    if(width  < pos.x+r && 0 < vel.x){ vel.x *= -1; }
    if(height < pos.y+r && 0 < vel.y){ vel.y *= -1; }
    if(pos.x-r < 0 && vel.x < 0){ vel.x *= -1; }
    if(pos.y-r < 0 && vel.y < 0){ vel.y *= -1; }
  }

  public void display(){
    ellipse(pos.x, pos.y, r*2, r*2);
  }
}

class BallManager{
  ArrayList<Ball> balls;
  MarchingSquare marchingSquare;

  BallManager(int n, int scale){
    balls = new ArrayList<Ball>();
    for(int i = 0; i < n; i++){
      balls.add(new Ball());
    }

    marchingSquare = new MarchingSquare(scale);
  }

  public float getSumDist(float x, float y){
    float sum = 0;
    for(Ball ball : balls){
      float bx = ball.pos.x;
      float by = ball.pos.y;
      sum += sq(ball.r) / (sq(bx-x) + sq(by-y));
    }
    sum /= 2;
    return sum;
  }

  public void update(boolean resize){
    for(Ball ball : balls){
      ball.update();
    }

    if(resize){
      int mx = max(1, min(mouseX, width));
      marchingSquare.resize(mx/8, mx/8);
    }

    marchingSquare.generateBallWorld(this);

    display();
  }

  public void display(){
    stroke(256, 128, 0); strokeWeight(1);
    background(0);
    fill(256, 128, 0);
    marchingSquare.displaySurface();
    //marchingSquare.displayArea();
  }
}
class InputManager{
  boolean mouseLDown = false;
  boolean mouseRDown = false;
  float mouseR = 100;
  MarchingSquare marchingSquare;

  InputManager(int w, int h){
    marchingSquare = new MarchingSquare(w, h);
    marchingSquare.generateNoiseWorld(5/(float)w);
  }
  InputManager(int size){
    marchingSquare = new MarchingSquare(size, size);
    marchingSquare.generateNoiseWorld(5/(float)size);
  }

  public void update(){
    if(mouseLDown){ marchingSquare.changeAreaSmooth(mouseX, mouseY, mouseR, -0.02f); }
    if(mouseRDown){ marchingSquare.changeAreaSmooth(mouseX, mouseY, mouseR,  0.02f); }

    background(0);

    stroke(255, 128, 0); strokeWeight(2);
    fill(255, 128, 0);
    marchingSquare.displayArea();
    //marchingSquare.displayNodes();

    noFill();
    stroke(255); strokeWeight(1);
    ellipse(mouseX, mouseY, 2*mouseR, 2*mouseR);
  }
}

public void mousePressed(){
  if(mouseButton == LEFT){ inputManager.mouseLDown = true; }
  if(mouseButton == RIGHT){ inputManager.mouseRDown = true; }
}
public void mouseReleased(){
  if(mouseButton == LEFT){ inputManager.mouseLDown = false; }
  if(mouseButton == RIGHT){ inputManager.mouseRDown = false; }
}
public void mouseWheel(MouseEvent e) {
  inputManager.mouseR *= pow(1.2f, e.getCount());
}

class MarchingSquare{
  // Variables
  float[][] density;
  int precX, precY;
  float treshold = 0.5f;
  float squareW, squareH;

  // Constructor
  MarchingSquare(int precX, int precY){
    this.precX = precX;
    this.precY = precY;

    density = new float[precX+1][precY+1];

    squareW = (float)width/precX;
    squareH = (float)height/precY;
  }
  MarchingSquare(int prec){
    this.precX = prec;
    this.precY = prec;

    density = new float[precX+1][precY+1];

    squareW = (float)width/precX;
    squareH = (float)height/precY;
  }

  // Update
  public void resize(int precX, int precY){
    this.precX = precX;
    this.precY = precY;

    density = new float[precX+1][precY+1];

    squareW = (float)width/precX;
    squareH = (float)height/precY;
  }

  public void generateRandomWorld(){
    for(int x = 0; x < precX+1; x++){
      for(int y = 0; y < precY+1; y++){
        density[x][y] = random(0, 1);
      }
    }
  }
  public void generateNoiseWorld(float noiseScale){
    for(int x = 0; x < precX+1; x++){
      for(int y = 0; y < precY+1; y++){
        density[x][y] = noise(x*noiseScale, y*noiseScale);
      }
    }
  }
  public void generateBallWorld(BallManager ballManager){
    for(int x = 0; x < precX+1; x++){
      for(int y = 0; y < precY+1; y++){
        density[x][y] = ballManager.getSumDist(x*squareW, y*squareH);
      }
    }
  }

  public void changeAreaSmooth(float cx, float cy, float r, float diff){
    for(int x = 0; x < precX+1; x++){
      for(int y = 0; y < precY+1; y++){
        float dist = sqrt(sq(x*squareW - cx) + sq(y*squareH - cy));
        if(dist < r){
          density[x][y] += diff * (1-dist/r);
          density[x][y] = max(0, min(density[x][y], 1));
        }
      }
    }
  }
  public void changeArea(float cx, float cy, float r, float diff){
    for(int x = 0; x < precX+1; x++){
      for(int y = 0; y < precY+1; y++){
        float dist = sqrt(sq(x*squareW - cx) + sq(y*squareH - cy));
        if(dist < r){
          density[x][y] += diff;
          density[x][y] = max(0, min(density[x][y], 1));
        }
      }
    }
  }

  // Display
  public float calculateScale(float v1, float v2){
    float scale = 0.5f;
    if(v1 != v2){ scale = map(0.5f, v1, v2, 0, 1); }
    if(scale < -1 || 1 < scale){ scale = 0.5f; }
    if(scale < 0){ scale += 1; }
    return scale;
  }

  public void drawArea(PVector p1, PVector p2, PVector p3){
    triangle(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y);
  }
  public void drawArea(PVector p1, PVector p2, PVector p3, PVector p4){
    quad(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, p4.x, p4.y);
  }
  public void drawMarchingSquare(float x, float y, float w, float h, int tx, int ty){
    float rScale = calculateScale(density[tx+1][ty  ], density[tx+1][ty+1]);
    float tScale = calculateScale(density[tx  ][ty  ], density[tx+1][ty  ]);
    float lScale = calculateScale(density[tx  ][ty  ], density[tx  ][ty+1]);
    float bScale = calculateScale(density[tx  ][ty+1], density[tx+1][ty+1]);

    PVector r = new PVector(x + w       , y + h*rScale);
    PVector t = new PVector(x + w*tScale, y           );
    PVector l = new PVector(x           , y + h*lScale);
    PVector b = new PVector(x + w*bScale, y + h       );
    PVector tr = new PVector(x+w, y  );
    PVector tl = new PVector(x  , y  );
    PVector bl = new PVector(x  , y+h);
    PVector br = new PVector(x+w, y+h);

    int type = 0;
    if(density[tx+1][ty  ] > treshold){ type += 1; }
    if(density[tx  ][ty  ] > treshold){ type += 2; }
    if(density[tx  ][ty+1] > treshold){ type += 4; }
    if(density[tx+1][ty+1] > treshold){ type += 8; }

    // 2 -- 1
    // |    |
    // 4 -- 8

    switch(type){
      case 0: break;
      case 1: drawArea(t, r, tr); break;
      case 2: drawArea(t, l, tl); break;
      case 3: drawArea(tl, tr, r, l); break;
      case 4: drawArea(l, b, bl); break;
      case 5: drawArea(l, b, bl); drawArea(t, r, tr); break;
      case 6: drawArea(t, b, bl, tl); break;
      case 7: drawArea(bl, tl, r, b); drawArea(tl, r, tr); break;
      case 8: drawArea(r, br, b); break;
      case 9: drawArea(t, tr, br, b); break;
      case 10: drawArea(r, br, b); drawArea(t, l, tl); break;
      case 11: drawArea(b, l, tl, br); drawArea(tl, tr, br); break;
      case 12: drawArea(l, r, br, bl); break;
      case 13: drawArea(l, t, tr, bl); drawArea(tr, br, bl); break;
      case 14: drawArea(t, r, br, tl); drawArea(br, tl, bl); break;
      case 15: drawArea(tr, br, bl, tl); break;
    }
  }
  public void displayArea(){
    for(int x = 0; x < precX; x++){
      for(int y = 0; y < precY; y++){
        drawMarchingSquare(x*squareW, y*squareH, squareW, squareH, x, y);
      }
    }
  }

  public void drawEdge(PVector p1, PVector p2){
    line(p1.x, p1.y, p2.x, p2.y);
  }
  public void drawMarchingSquareEdge(float x, float y, float w, float h, int tx, int ty){
    float rScale = calculateScale(density[tx+1][ty  ], density[tx+1][ty+1]);
    float tScale = calculateScale(density[tx  ][ty  ], density[tx+1][ty  ]);
    float lScale = calculateScale(density[tx  ][ty  ], density[tx  ][ty+1]);
    float bScale = calculateScale(density[tx  ][ty+1], density[tx+1][ty+1]);

    PVector r = new PVector(x + w       , y + h*rScale);
    PVector t = new PVector(x + w*tScale, y           );
    PVector l = new PVector(x           , y + h*lScale);
    PVector b = new PVector(x + w*bScale, y + h       );

    int type = 0;
    if(density[tx+1][ty  ] > treshold){ type += 1; }
    if(density[tx  ][ty  ] > treshold){ type += 2; }
    if(density[tx  ][ty+1] > treshold){ type += 4; }
    if(density[tx+1][ty+1] > treshold){ type += 8; }

    // 2 -- 1
    // |    |
    // 4 -- 8

    switch(type){
      case 0: break;
      case 1: drawEdge(t, r); break;
      case 2: drawEdge(t, l); break;
      case 3: drawEdge(r, l); break;
      case 4: drawEdge(l, b); break;
      case 5: drawEdge(l, b); drawEdge(t, r); break;
      case 6: drawEdge(t, b); break;
      case 7: drawEdge(b, r); break;
      case 8: drawEdge(b, r); break;
      case 9: drawEdge(t, b); break;
      case 10: drawEdge(l, b); drawEdge(t, r); break;
      case 11: drawEdge(l, b); break;
      case 12: drawEdge(l, r); break;
      case 13: drawEdge(t, l); break;
      case 14: drawEdge(t, r); break;
      case 15: break;
    }
  }
  public void displaySurface(){
    for(int x = 0; x < precX; x++){
      for(int y = 0; y < precY; y++){
        drawMarchingSquareEdge(x*squareW, y*squareH, squareW, squareH, x, y);
      }
    }
  }

  public void displayNodes(){
    for(int x = 0; x < precX+1; x++){
      for(int y = 0; y < precY+1; y++){
        fill(map(density[x][y], 0, 1, 0, 255));
        //if(density[x][y] < treshold){ fill(255); }else{ fill(0); }
        ellipse(x * squareW, y * squareH, 10, 10);
      }
    }
  }
}
  public void settings() {  size(800, 800); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "MarchingCubes" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
