import tester.Tester;
import javalib.funworld.*;
import javalib.worldcanvas.WorldCanvas;
import javalib.worldimages.*;
import java.awt.Color;
import java.util.Random;

interface IFish {
  // is this fish inside that fish
  boolean isInside(IFish that);
  WorldImage fishImage();
  Coord getPosition();
  int getSize();
  IFish moveFish(int x, int y);
  IFish PlayerMove(String ke);
  PlayerFish eat(IFish other);
}

interface ILoFish {
  WorldImage drawLoFish();
  WorldImage drawLoFishAcc(WorldImage acc);
  ILoFish shift(int x);
  ILoFish removeFish(IFish fish);
  IFish checkEat(PlayerFish player);
  ILoFish respawnFish(IFish fish);
}

class MtLoFish implements ILoFish {
  MtLoFish() { 
    
  }
  
  public WorldImage drawLoFish() {
    return new RectangleImage(500, 500, OutlineMode.SOLID, Color.CYAN);
  }
  
  public WorldImage drawLoFishAcc(WorldImage acc) {
    return acc;
  }
  
  public ILoFish shift(int x) {
    return this;
  }
  
  public ILoFish removeFish(IFish fish) {
    return this;
  }
  
  public IFish checkEat(PlayerFish player) {
    return player;
  }
  
  public ILoFish respawnFish(IFish fish) {
    return this;
  }
}

class ConsLoFish implements ILoFish {
  IFish first;
  ILoFish rest;
  
  ConsLoFish(IFish first, ILoFish rest) {
    this.first = first;
    this.rest = rest;
  }
  
  public WorldImage drawLoFish() {
    return this.drawLoFishAcc(new RectangleImage(550, 550, OutlineMode.SOLID, Color.CYAN));
  }
  
  public WorldImage drawLoFishAcc(WorldImage acc) {
    return 
        this.rest.drawLoFishAcc(new OverlayOffsetImage(
          this.first.fishImage(),
          250 - this.first.getPosition().x,
          250 - this.first.getPosition().y,
          acc));
        
  }
  
  public ILoFish shift(int x) {
    return new ConsLoFish(this.first.moveFish(x, 0), this.rest.shift(x));
  }
  
  public ILoFish removeFish(IFish fish) {
    if (this.first.equals(fish)) {
      return this.rest;
    }
    else {
      return new ConsLoFish(this.first, this.rest.removeFish(fish));
    }
  }
  
  public IFish checkEat(PlayerFish player) {
    if (player.isInside(this.first) && this.first.getSize() < player.getSize()) {
      return this.first;
    }
    else {
      return this.rest.checkEat(player);
    }
  }
  
  public ILoFish respawnFish(IFish fish) {
    return new ConsLoFish(
        new BGFish(
            new Coord(479, (int)(Math.random() * 500)), 
            (int)(Math.random() * 20) + 10, 
            Color.YELLOW), 
        this.removeFish(fish));
  }
  
}

class Coord {
  int x;
  int y;
  
  Coord(int x, int y) {
    this.x = x;
    this.y = y;
  }
  
  // determines this coord is within the given range of another posn
  double distance(Coord other) {
    return Math.sqrt((this.x - other.x) * (this.x - other.x) + (this.y - other.y) * (this.y - other.y));
  }
  
  // moves this
  Coord moveCoord(int xIncrement, int yIncrement) {
    return new Coord(this.x + xIncrement, this.y + yIncrement);
  }
}

class FishyWorld extends World {
  int height = 500;
  int width = 500;
  IFish fish;
  ILoFish BGFish;
  
  public FishyWorld(IFish fish, ILoFish BGFish) {
    super();
    this.fish = fish;
    this.BGFish = BGFish;
  }
  
  public WorldScene makeScene() {
    return this.getEmptyScene()
        .placeImageXY(BGFish.drawLoFish(), 250, 250)
        .placeImageXY(fish.fishImage(), 
        this.fish.getPosition().x, 
        this.fish.getPosition().y);
        
  }
  
  // Move the fish when the player presses a key 
  public World onKeyEvent(String ke) {
    return new FishyWorld(this.fish.PlayerMove(ke), this.BGFish);
  }

  public World onTick() {
    if (this.BGFish.checkEat((PlayerFish)this.fish).equals((PlayerFish)this.fish)) {
      return new FishyWorld(this.fish, this.BGFish.shift(-2));
    }
    else {
      return new FishyWorld(
          this.fish.eat(this.BGFish.checkEat((PlayerFish)this.fish)),
          this.BGFish.respawnFish(this.BGFish.checkEat((PlayerFish)this.fish)).shift(-2));
    }
  }

  
  public IFish genRandBGFish() {
    return new BGFish(
        new Coord((int)(Math.random() * 500), (int)(Math.random() * 500)), 
        (int)(Math.random() * 20) + 10, 
        Color.YELLOW);
  }
  
  public ILoFish genLoFish(int n) {
    if (n > 0) {
      return new ConsLoFish(this.genRandBGFish(), this.genLoFish(n - 1));
    }
    else {
      return new MtLoFish();
    }
  }
  
  
}

abstract class Fish implements IFish {
  Coord coord;
  int size;
  Color c;
  
  // constructor
  Fish(Coord coord, int size, Color c) {
    this.coord = coord;
    this.size = size;
    this.c = c;
  }
  
  public int getSize() {
    return this.size;
  }
  
  public Coord getPosition() {
    return this.coord;
  }
  
  public boolean isInside(IFish that) {
    return (this.coord.distance(that.getPosition()) - that.getSize() / 2) < (this.size / 2);
  }
  
  public WorldImage fishImage() {
    return new BesideAlignImage(
        AlignModeY.MIDDLE,
        new EllipseImage(this.size * 2, this.size, OutlineMode.SOLID, c),
        new RotateImage(new EquilateralTriangleImage(this.size, OutlineMode.SOLID, this.c), 270));
  }
  public abstract IFish PlayerMove(String ke);
  
  public IFish moveFish(int x, int y) {
    if (this.getPosition().x > 490) {
      return new PlayerFish(coord.moveCoord(-480, 0), this.size, this.c);
    }
    else if (this.getPosition().x < 10) {
      return new PlayerFish(coord.moveCoord(480, 0), this.size, this.c);
    }
    else if (this.getPosition().y > 490) {
      return new PlayerFish(coord.moveCoord(0, -480), this.size, this.c);
    }
    else if (this.getPosition().y < 10) {
      return new PlayerFish(coord.moveCoord(0, 480), this.size, this.c);
    }
    else {
      return new PlayerFish(coord.moveCoord(x, y), this.size, this.c);
    }
  }
}

class PlayerFish extends Fish {
  // constructor
  PlayerFish(Coord coord, int size, Color c) {
    super(coord, size, c);
  }
  
  public PlayerFish eat(IFish other) {
    return new PlayerFish(this.coord, this.size + other.getSize() / 3, this.c);
  }
  
  boolean isEaten(IFish other) {
    return isInside(other) && this.size < other.getSize();
  }
  

  
  public IFish PlayerMove(String ke) {
    if (ke.equals("right")) {
        return this.moveFish(5, 0);
    } 
    else if (ke.equals("left")) {
        return this.moveFish(-5, 0);
    } 
    else if (ke.equals("up")) {
        return this.moveFish(0, -5);
    } 
    else if (ke.equals("down")) {
        return this.moveFish(0,  5);
    }
    else {
        return this;
    }
  }
}

class BGFish extends Fish {
  // constructor
  BGFish(Coord coord, int size, Color c) {
    super(coord, size, c);
  }
  
  public IFish PlayerMove(String ke) {
    return this;
  }
  
  public PlayerFish eat(IFish other) {
    return null;
  }
  
}
class ExamplesFish {
  Coord coord1 = new Coord(10, 10);
  Coord coord2 = new Coord(20, 20);
  
  FishyWorld w = new FishyWorld(this.PlayerFish1, this.BGFishList);
  
  IFish BGFish1 = new BGFish(new Coord(40, 40), 30, Color.BLUE);
  IFish BGFish2 = new BGFish(new Coord(30, 60), 40, Color.RED);
  IFish BGFish3 = new BGFish(new Coord(100, 100), 20, Color.GREEN);
  IFish PlayerFish1 = new PlayerFish(new Coord(250, 250), 20, Color.RED);
  
  ILoFish BGFishList = this.w.genLoFish(8);
  ILoFish BGFishList2 = new ConsLoFish(this.BGFish1, 
      new ConsLoFish(this.BGFish2, 
          new MtLoFish()));
  
  boolean testFishyWorld(Tester t) {  
    // run the game
    //FishyWorld w = new FishyWorld(this.PlayerFish1, new ConsLoFish(this.BGFish1, new MtLoFish()));
    FishyWorld w = new FishyWorld(this.PlayerFish1, this.BGFishList);
    return w.bigBang(500, 500, 0.1);
  }
  
  boolean testInRange(Tester t) {
    return t.checkInexact(this.coord1.distance(this.coord2), 14.14, 0.001);
  }
  
  boolean testIsInside(Tester t) {
    return t.checkExpect(this.BGFish1.isInside(BGFish2), true)
        && t.checkExpect(this.BGFish1.isInside(BGFish3), false);
        // && t.checkExpect(this.BGFishList, null);
  }
  
  boolean testRemoveFish(Tester t) {
    return t.checkExpect(this.BGFishList2.removeFish(this.BGFish1), 
        new ConsLoFish(this.BGFish2, new MtLoFish()))
        && t.checkExpect(this.BGFishList2.removeFish(this.BGFish2), 
            new ConsLoFish(this.BGFish1, new MtLoFish()));
  }
/*
  boolean testDrawFish(Tester t) {
    WorldCanvas c = new WorldCanvas(500, 500);
    WorldScene s = new WorldScene(500, 500);
    return c.drawScene(s.placeImageXY(this.BGFishList.drawLoFish(), 250, 250))
        && c.show();
  }
*/
}
