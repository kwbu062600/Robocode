package s180209805;
import robocode.*;
import java.io.*;
import java.util.*;
import java.awt.event.*;
import java.awt.Color;
import java.awt.geom.Point2D;

// API help : http://robocode.sourceforge.net/docs/robocode/robocode/Robot.html

public class Task3 extends AdvancedRobot
{
	public enum State{
		idle,
		findingSides,
		patrolling,
		meleeing,
		chasing,
		attack,
		squelch,
	}



	public enum Region{
		Region1,
		Region2,
		Region3,
		Region4,
	}

	public State state;
	public Region region;
	
	AdvancedEnemyBot enemy = new AdvancedEnemyBot();
	robocode.Event temp , temp1;
	boolean updated;
	byte moveDirection = 1;
	double firePower;
	double limitedSizeLeft, limitedSizeBottom, limitedSizeTop, limitedSizeRight;
	double centerX, centerY;
	String hitRobotName;
	double guardDistance;
	boolean inside;
	Color color , color2;
	public void run() {
		//initialize state
		state = State.idle;	

		// find the limit size the bot move in 
		limitedSizeLeft = getSentryBorderSize();
		limitedSizeBottom = getSentryBorderSize() ;
		limitedSizeRight = getBattleFieldWidth()-getSentryBorderSize();
		limitedSizeTop = getBattleFieldHeight()-getSentryBorderSize(); 		

		//center Point of board
		centerX = getBattleFieldWidth()/2;
		centerY = getBattleFieldHeight()/2;

		
		//setBodyColor(new Color(47, 54, 64));
		//setGunColor(new Color(127, 184, 159));
		//setRadarColor(Color.RED);
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForRobotTurn(true);
		setAdjustRadarForGunTurn(true);

		while(true) { 	
			// Appearance of bot
			color = new Color( ((int)(Math.random()*118))+138 , ((int)(Math.random()*118))+138 , ((int)(Math.random()*118))+138 );
			color2 = new Color( ((int)(Math.random()*255)) , ((int)(Math.random()*255)) , ((int)(Math.random()*255)) );
	 		setBodyColor(color);
			setScanColor(color2);
			setGunColor(Color.BLACK);
			setRadarColor(color);
			setBulletColor(color2);
			// check whether inside the limited board
			if(getX() > limitedSizeLeft &&	getY() > limitedSizeBottom && 	getX() < limitedSizeRight && getY() < limitedSizeTop)
			{
				inside = true;
			}

			else{
				inside = false;
			}
			
			if(!inside){		
				if(state != state.chasing && state != State.squelch && state != State.meleeing)
					state = State.findingSides;
			}
			else {
				if(state != State.attack && state != State.chasing && state != State.squelch && state != State.meleeing){
					state = State.patrolling;
				}
				
			}
			// do each of action of state
			switch (state){
				case idle:	
					break;	
				case findingSides:

					findBoarder();
					break;

				case patrolling:
					scanTarget();
					patrol();
					break;
				
				case meleeing:	
					scanTarget();
					meleeing();
					break;

				case chasing:
					scanTarget();
					moveToTarget();	
					break;

				case attack:	
					scanTarget();				
					doMove();
					attackMode();
					
					break;		
				case squelch:
					scanTarget();
					squelchMode();					
					break;		
			}

			scan();									// wake up radar if not working
			updated = false;
			execute();
			
			out.println("Current State = "  + state);
		
			out.println("enemy name = " + enemy.getName());
			out.println("enemy distance = " + enemy.getDistance());
			out.println("inside = " + inside);
		}
	}

	/**
	 * onScannedRobot: What to do when you see another robot
	 */

	public void onScannedRobot(ScannedRobotEvent e) {	
		// if it is a sentry bot , ignore it 
		if(e.isSentryRobot()){
			return;
		}
		// doing each of the transition of state
		else{
			if(state != State.squelch){
				if(e.getName().equals(hitRobotName) && e.getEnergy()<20){
					state = State.squelch;
					enemy.update(e,this);				
				}
			}
			
			if(enemy.none() && state != State.chasing && e.getDistance()<=200){
				enemy.update(e,this);
			}
			else if(enemy.none() && state == State.chasing){
				enemy.update(e,this);
			}

			if(state == State.attack || state == State.chasing || state == State.squelch ||
				state == State.meleeing || state == State.squelch || state == State.patrolling){
				if(e.getName().equals(enemy.getName())){	
					enemy.update(e,this);
				}
				if(enemy.getDistance() > 280 && state != State.chasing && state != State.squelch && state != State.meleeing){
					state = State.patrolling;
					enemy.reset();
					scanTarget();
				}

				if(e.getDistance() < enemy.getDistance() && e.getDistance()<50){
					enemy.update(e,this);
				}

			}
			
			if(getOthers() <= 2){
		
				state = State.chasing;
				
			}	

			if(e.getDistance() <= 150 && e.getEnergy() <= 35 && state != State.chasing && state != State.squelch )
			{	
				state = State.meleeing;
				enemy.update(e,this);	
			}
			// added new 
			if(e.getDistance() <= 150 && state == State.patrolling)
			{
				state = State.attack;
			}
		}
	}
	// when someone bullet hit the bot , doing squelch 
	public void onHitByBullet(HitByBulletEvent event){
		hitRobotName = event.getName();
	}

	public void onRobotDeath(RobotDeathEvent e) {
		// check if the enemy we were tracking died
		if (e.getName().equals(enemy.getName())) {
			enemy.reset();
			state = State.patrolling;
			out.println("Reset");		
		}
		scanTarget();
		out.println("SCANNED");	
	}


	// when the bot hit others enemy
	public void onHitRobot(HitRobotEvent event) {
       if (event.getBearing() > -90 && event.getBearing() <= 90) {
           setBack(50);
       } else {
           setAhead(50);
       }
	   execute();
   }

	// move
	public void doMove(){
		if(state == State.squelch)
			setAhead(enemy.getDistance()/2 - 70);
		else if (getTime() % 5 == 0) {
			moveDirection *= -1;
			setAhead(500 * moveDirection);
		}
		out.println("Time = "  + getTime());
	}

	// gun turn and fire here
	public void doGun(){
		firePower = Math.min(300 / enemy.getDistance(), 3);	
		double bulletSpeed = 20 - firePower * 3;
		long time = (long)(enemy.getDistance() / bulletSpeed);
		double futureX = enemy.getFutureX(time);
		double futureY = enemy.getFutureY(time);
		double absDeg = absoluteBearing(getX(), getY(), futureX, futureY);
			
		setTurnGunRight(normalizeBearing(absDeg - getGunHeading()));	
		
		if (getGunHeat() == 0 && Math.abs(getGunTurnRemaining()) <= 5 ) {
			setFire(firePower);
		}
		
		execute();
	}

	// scan any enemy
	public void scanTarget() {
		setTurnRadarRight(360);
	}
	
	/*
	public void lockRadar() {
		enemy.update((ScannedRobotEvent)(temp),this);
		setTurnRadarRight(robocode.util.Utils.normalRelativeAngleDegrees(
		getHeading() - getRadarHeading() + enemy.getBearing()));
		
	}
	*/

	// turn tank
	public void turnTank() {
		if(state == State.chasing || state == State.meleeing || state == State.squelch)
			setTurnRight(normalizeBearing(enemy.getBearing()));
		else
			setTurnRight(normalizeBearing(enemy.getBearing() + 90 - (15 * moveDirection)));

	}

	// chasing enemy
	public void moveToTarget() {
		//attackMode();
		if(getEnergy() >= 30){
			turnTank();
			doGun();
			setAhead(enemy.getDistance());
		}
		else{
			//attackMode();
			state = State.attack;
		}
		//execute();	
	
	}
	
	// do patrol
	public void patrol(){
		setTurnRight(normalizeBearing( ((int)(Math.random()*359)+1) + 90 - (15 * moveDirection)));
		setAhead(Math.hypot( (centerY-getY()) , (centerX - getX()) ) );		
	}

	// meleeing enemy
	public void meleeing(){
		scanTarget();
		moveToTarget();
	}

	// squelch
	public void squelchMode(){
		scanTarget();
		doMove();
		attackMode();	
	}

	// attack
	public void attackMode(){
		turnTank();
		doGun();		
	}

	// in the sentry bot , find side
	public void findBoarder(){
		double up, down, left , right;
		double distance;
		double turn = 0;

		// identify the region area 
		if(getX() < centerX && getY() > centerY)
			region = Region.Region1;
		else if(getX() > centerX && getY() > centerY)
			region = Region.Region2;
		else if(getX() < centerX && getY() < centerY)
			region = Region.Region3;
		else if(getX() > centerX && getY() < centerY)
			region = Region.Region4;


		// determine inside and outside direction move
		if(inside){
			up = limitedSizeTop - getY();
			down = getY() - limitedSizeBottom;
			left = getX() - limitedSizeLeft;
			right = limitedSizeRight - getX();
		}
		else{
			up = limitedSizeBottom - getY();
			down = getY() - limitedSizeTop; 
			left = getX() - limitedSizeRight;
			right =	limitedSizeLeft - getX();
		}


			switch(region){
				case Region1:
					// inside
					if(inside){
						if(left < up){
							turn = normalizeBearing(270 - getHeading());
							
							if(turn != 0)
								setTurnRight(turn);
							else
								setAhead(left-20);
							out.println("tank turn left = "  + turn);
						}
						else{
							turn = normalizeBearing(0 - getHeading());

							if(turn != 0)
								setTurnRight(turn);		
							else
								setAhead(up-20);
							out.println("tank turn left = "  + turn);
						}		
					}
				
					// outside
					else {
						if(getX()<limitedSizeLeft && getY() < limitedSizeTop){
							turn = normalizeBearing(90 - getHeading());

							if(turn != 0)
								setTurnRight(turn);		
							else
								setAhead(right+20);
							out.println("tank turn left = "  + turn);
						}
						else if (getX() > limitedSizeLeft && getY()> limitedSizeTop){
							turn = normalizeBearing(180 - getHeading());
	
							if(turn != 0)
								setTurnRight(turn);		
							else
								setAhead(down+20);
							out.println("tank turn left = "  + turn);
						}
						else if( getX() < limitedSizeLeft && getY()>limitedSizeTop){
							distance = Math.hypot((limitedSizeLeft-getX()), (limitedSizeTop- getY()));
							turn = normalizeBearing(135 - getHeading());

							if(turn != 0)
								setTurnRight(turn);		
							else
								setAhead(distance+20);
							out.println("tank turn left = "  + turn);
						}
					}	
					execute();
					break;
				case  Region2:
					if(inside){
						if(right < up)	{
							turn = normalizeBearing(90 - getHeading());

							if(turn != 0)
								setTurnRight(turn);		
							else
								setAhead(right-20);
							out.println("tank turn left = "  + turn);
						}
						else{
							turn = normalizeBearing(0 - getHeading());

							if(turn != 0)
								setTurnRight(turn);		
							else
								setAhead(up-20);
							out.println("tank turn left = "  + turn);
						}	
					}
					else{
						if(getX()>limitedSizeRight && getY() < limitedSizeTop){
							turn = normalizeBearing(270 - getHeading());

							if(turn != 0)
								setTurnRight(turn);		
							else
								setAhead(left+20);
							out.println("tank turn left = "  + turn);
						}
						else if (getX() < limitedSizeRight && getY()> limitedSizeTop){
							turn = normalizeBearing(180 - getHeading());

							if(turn != 0)
								setTurnRight(turn);		
							else
								setAhead(down+20);
							out.println("tank turn left = "  + turn);
						}
						else if( getX() > limitedSizeRight && getY()>limitedSizeTop){
							distance = Math.hypot((limitedSizeRight-getX()), (limitedSizeTop- getY()));
							turn = normalizeBearing(225 - getHeading());
							if(turn != 0)
								setTurnRight(turn);		
							else
								setAhead(distance+20);
							out.println("tank turn left = "  + turn);
						}

					}
					execute();
					break;

				case Region3:
					if(inside){
						if(left < down){
							turn = normalizeBearing(270 - getHeading());
							if(turn != 0)
								setTurnRight(turn);		
							else
								setAhead(left-20);
							out.println("tank turn left = "  + turn);
						}
						else{
							turn = normalizeBearing(180 - getHeading());

							if(turn != 0)
								setTurnRight(turn);		
							else
								setAhead(down-20);
							out.println("tank turn left = "  + turn);
						}
					}
					else{
						if(getX() < limitedSizeLeft  && getY() > limitedSizeBottom){
							turn = normalizeBearing(90 - getHeading());
							if(turn != 0)
								setTurnRight(turn);		
							else
								setAhead(right+20);				
							out.println("tank turn left = "  + turn);
						}
						else if (getX() > limitedSizeLeft && getY() < limitedSizeBottom){
							turn = normalizeBearing(360 - getHeading());
							if(turn != 0)
								setTurnRight(turn);		
							else				
								setAhead(up+20);		
							out.println("tank turn left = "  + turn);
						}
						else if( getX() < limitedSizeLeft && getY() < limitedSizeBottom){
							distance = Math.hypot((limitedSizeLeft-getX()), (limitedSizeBottom- getY()));
							turn = normalizeBearing(45 - getHeading());
							if(turn != 0)
								setTurnRight(turn);		
							else
								setAhead(distance+20);
							out.println("tank turn left = "  + turn);
						}
					}
					execute();
					break;
				case Region4:
					if(inside){
						if(right < down){
							turn = normalizeBearing(90 - getHeading());				
							if(turn != 0)
								setTurnRight(turn);		
							else
								setAhead(right-20);
							out.println("tank turn left = "  + turn);
						}
						else{
							turn = normalizeBearing(180 - getHeading());
							if(turn != 0)
								setTurnRight(turn);		
							else
								setAhead(down-20);
							out.println("tank turn left = "  + turn);
						}
					}else{
						if(getX()> limitedSizeRight && getY() > limitedSizeBottom){
							turn = normalizeBearing(270 - getHeading());
							if(turn != 0)
								setTurnRight(turn);		
							else
								setAhead(left+20);
							out.println("tank turn left = "  + turn);
						}
						else if (getX() < limitedSizeRight && getY() < limitedSizeBottom){
							turn = normalizeBearing(0 - getHeading());
							if(turn != 0)
								setTurnRight(turn);		
							else
								setAhead(up+20);
							out.println("tank turn left = "  + turn);
						}
						else if( getX() > limitedSizeRight && getY() < limitedSizeBottom){
							distance = Math.hypot((limitedSizeRight-getX()), (limitedSizeBottom- getY()));
							turn = normalizeBearing(315 - getHeading());
							if(turn != 0)
								setTurnRight(turn);		
							else
								setAhead(distance+20);
							out.println("tank turn left = "  + turn);
						}
					}
					execute();
					break;
			}
			out.println("inside = " + inside);
			out.println("region = " + region);
			out.println("centerX = " + centerX);
			out.println("centerY = " + centerY);
			out.println("X = " + getX());
			out.println("Y = " + getY());
			out.println("up = " + up + ", down = " + down + ", left = " + left + ", right = " + right);
	}

	/*
	public void changeDirection(){
		
		setBack(1000);
	}
*/
/*
	public void onHitWall(HitWallEvent e){

		state = State.crush;
		temp1 = e;
		
		//changeDirection();
	}
*/
// computes the absolute bearing between two points
	double absoluteBearing(double x1, double y1, double x2, double y2) {
		double xo = x2-x1;
		double yo = y2-y1;
		double hyp = Point2D.distance(x1, y1, x2, y2);
		double arcSin = Math.toDegrees(Math.asin(xo / hyp));
		double bearing = 0;

		if (xo > 0 && yo > 0) { // both pos: lower-Left
			bearing = arcSin;
		} else if (xo < 0 && yo > 0) { // x neg, y pos: lower-right
			bearing = 360 + arcSin; // arcsin is negative here, actually 360 - ang
		} else if (xo > 0 && yo < 0) { // x pos, y neg: upper-left
			bearing = 180 - arcSin;
		} else if (xo < 0 && yo < 0) { // both neg: upper-right
			bearing = 180 - arcSin; // arcsin is negative here, actually 180 + ang
		}

		return bearing;
	}

	double normalizeBearing(double angle) {
		while (angle >  180) angle -= 360;
		while (angle < -180) angle += 360;
		return angle;
	}

}

class EnemyBot {

	// private data
	private double bearing;
	private double distance;
	private double energy;
	private double heading;
	private String name;
	private double velocity;

	// constructor
	public EnemyBot() {
		reset();
	}

	// mutator / state methods
	public void reset() {
		bearing = 0.0;
		distance = 0.0;
		energy = 0.0;
		heading = 0.0;
		name = "";
		velocity = 0.0;
	}

	final public void update(ScannedRobotEvent e) {
		bearing = e.getBearing();
		distance = e.getDistance();
		energy = e.getEnergy();
		heading = e.getHeading();
		name = e.getName();
		velocity = e.getVelocity();
	}

	public boolean shouldTrack(ScannedRobotEvent e, long closer) {
		return  none() || e.getDistance() < getDistance() - 70 ||
				e.getName().equals(getName());
	}

	public boolean none() { return name.equals(""); }

	// accessor methods
	public double getBearing()  { return bearing; }
	public double getDistance() { return distance; }
	public double getEnergy()   { return energy; }
	public double getHeading()  { return heading; }
	public String getName()     { return name; }
	public double getVelocity() { return velocity; }

}
class AdvancedEnemyBot extends EnemyBot {

	// constructor
	public AdvancedEnemyBot() {
		reset();
	}

	public void reset() {
		// tell parent to reset all his stuff
		super.reset();
		// now update our stuff
		x = 0.0;
		y = 0.0;
	}

	public void update(ScannedRobotEvent e, Robot robot) {
		// tell parent to update his stuff
		super.update(e);

		// now update our stuff

		// (convenience variable)
		double absBearingDeg = (robot.getHeading() + e.getBearing());
		if (absBearingDeg < 0) absBearingDeg += 360;

		// yes, you use the _sine_ to get the X value because 0 deg is North
		x = robot.getX() + Math.sin(Math.toRadians(absBearingDeg)) * e.getDistance();

		// likewise, you use the _cosine_ to get the Y value for the same reason
		y = robot.getY() + Math.cos(Math.toRadians(absBearingDeg)) * e.getDistance();
	}

	// accessor methods
	public double getX() { return x; }
	public double getY() { return y; }

	public double getFutureX(long when) {
		/*
		double sin = Math.sin(Math.toRadians(getHeading()));
		double futureX = x + sin * getVelocity() * when;
		return futureX;
		*/
		return x + Math.sin(Math.toRadians(getHeading())) * getVelocity() * when;
	}

	public double getFutureY(long when) {
		/*
		double cos = Math.cos(Math.toRadians(getHeading()));
		double futureY = y + cos * getVelocity() * when;
		return futureY;
		*/
		return y + Math.cos(Math.toRadians(getHeading())) * getVelocity() * when;
	}
	
	private double x;
	private double y;
}


