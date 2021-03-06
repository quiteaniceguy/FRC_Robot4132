
package org.usfirst.frc.team4132.robot;

import edu.wpi.first.wpilibj.CANJaguar;
import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.first.wpilibj.Compressor;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.Jaguar;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.RobotDrive;
import edu.wpi.first.wpilibj.RobotDrive.MotorType;
import edu.wpi.first.wpilibj.Servo;
import edu.wpi.first.wpilibj.Spark;
import edu.wpi.first.wpilibj.Ultrasonic;
import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.command.Scheduler;
import edu.wpi.first.wpilibj.livewindow.LiveWindow;

import org.usfirst.frc.team4132.robot.Robot.PistonStates_t;
import org.usfirst.frc.team4132.robot.commands.ExampleCommand;
import org.usfirst.frc.team4132.robot.subsystems.ExampleSubsystem;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the IterativeRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the manifest file in the resource
 * directory.
 */
public class Robot extends IterativeRobot {
	
	public static final ExampleSubsystem exampleSubsystem = new ExampleSubsystem();
	private final int BUTTONCOUNT=4;
	private final int A=1,B=2,X=3,Y=4, LTRIGGER=5,RTRIGGER=6;
	private final int CAMERAPORT=1;
	private final int[]	PICKUPSOLENOIDPORT={6,7};
	private final int[] ROBOTDRIVEIDS={1,0};
	private final int SPINBARID=2;
	///using can
	private final int SHOOTERIDS[]={1,2};
	private final int[] BALLSONARID={6,7};
	private final int[] OBSTACLESONARID={4,5};
	public static OI oi;
	private int buttonInitPressed=-1;
	private int numberOfInitButtonsPressed=0;
	
	

    Command autonomousCommand;
    SendableChooser chooser;
    
    private Ultrasonic ballSonar;
    private Ultrasonic obstacleSonar;
    
    private RobotDrive myRobot;
    private Joystick controller;
    private CANTalon shooter1;
    private CANTalon shooter2;
    private DoubleSolenoid pickUpSolenoid;
    private Spark spinbar;
    
    CameraServer server;
    
    public Button[] Buttons=new Button[BUTTONCOUNT];
    public boolean[] Edges=new boolean[BUTTONCOUNT];
    
    
    
    enum PistonStates_t{
		IDLE,PISTONOUT,WAIT,PISTONIN
	}
    PistonStates_t pickUpPistonState=PistonStates_t.IDLE;
    
    enum ShooterStates_t{
    	REVERSE, FORWARD, IDLE
    }
    private ShooterStates_t shooterState;
    private int shooterTimer;
    
    enum PortcullisAutoStates_t{
    	FORWARD1, STOP, LIFT, FORWARD2, IDLE;
    }
    private PortcullisAutoStates_t portcullisAutoState=PortcullisAutoStates_t.FORWARD1;
    private int portcullisAutoCounter=0;
    public static int autonomousLoopCounter=0;
    Servo cameraServo;
    
    Compressor c=new Compressor(0);
    
    private SmartDashboard dashboard;

   
    private double leftXAxis;
    private double leftYAxis;
    private double rightXAxis;
    private double rightYAxis;
    

    /**
     * This function is run when the robot is first started up and should be
     * used for any initialization code.
     */
    ///runs the first time the robot is uploaded, or when the driver station turns on
    public void robotInit() {
    	c.setClosedLoopControl(true);
    	
    	for(int i=0;i<Buttons.length;i++){
    		Buttons[i]=new Button(i, controller);
    		Edges[i]=false;
    	}
    	
		
//        chooser.addObject("My Auto", new MyAutoCommand());
        SmartDashboard.putData("Auto mode", chooser);
        
        myRobot=new RobotDrive(new Spark(ROBOTDRIVEIDS[0]),new Spark(ROBOTDRIVEIDS[1]));
        server=CameraServer.getInstance();
        server.setQuality(50);
        server.startAutomaticCapture("cam0");
        
        pickUpSolenoid=new DoubleSolenoid(PICKUPSOLENOIDPORT[0],PICKUPSOLENOIDPORT[1]);
        
		pickUpSolenoid.set(DoubleSolenoid.Value.kForward);
		pickUpPistonState=PistonStates_t.PISTONOUT;
		
		cameraServo=new Servo(CAMERAPORT);
		
        ballSonar=new Ultrasonic(BALLSONARID[0],BALLSONARID[1]);
        ballSonar.setAutomaticMode(true);
        obstacleSonar=new Ultrasonic(OBSTACLESONARID[0],OBSTACLESONARID[1]);
        obstacleSonar.setAutomaticMode(true);
        
        shooter1=new CANTalon(SHOOTERIDS[0]);
        shooter2=new CANTalon(SHOOTERIDS[1]);
        shooter2.setInverted(true);
        
        spinbar=new Spark(SPINBARID);
        
        dashboard=new SmartDashboard();
        //myRobot.setInvertedMotor(MotorType.kFrontLeft, true);
        //myRobot.setInvertedMotor(MotorType.kFrontLeft, true);
    }
	
	/**
     * This function is called once each time the robot enters Disabled mode.
     * You can use it to reset any subsystem information you want to clear when
	 * the robot is disabled.
     */
    public void disabledInit(){

    }
	
	public void disabledPeriodic() {
		Scheduler.getInstance().run();
	}

	/**
	 * This autonomous (along with the chooser code above) shows how to select between different autonomous modes
	 * using the dashboard. The sendable chooser code works with the Java SmartDashboard. If you prefer the LabVIEW
	 * Dashboard, remove all of the chooser code and uncomment the getString code to get the auto name from the text box
	 * below the Gyro
	 *
	 * You can add additional auto modes by adding additional commands to the chooser code above (like the commented example)
	 * or additional comparisons to the switch structure below with additional strings & commands.
	 */
    public void autonomousInit() {
    	autonomousLoopCounter=0;
    	boolean[] buttonValues = {SmartDashboard.getBoolean("DB/Button 0", false),
    			SmartDashboard.getBoolean("DB/Button 1", false),
    			SmartDashboard.getBoolean("DB/Button 2", false),
    			SmartDashboard.getBoolean("DB/Button 3", false)};
    	for(int i=0;i<buttonValues.length;i++){
    		if(buttonValues[i]==true){
    			buttonInitPressed=i;
    			numberOfInitButtonsPressed++;
    		}
    	}
    	
    	portcullisAutoCounter=0;
    	portcullisAutoState=PortcullisAutoStates_t.FORWARD1;
    	
        autonomousCommand = (Command) chooser.getSelected();  
		/* String autoSelected = SmartDashboard.getString("Auto Selector", "Default");
		switch(autoSelected) {
		case "My Auto":
			autonomousCommand = new MyAutoCommand();
			break;
		case "Default Auto":
		default:
			autonomousCommand = new ExampleCommand();
			break;
		} */
    	
    	// schedule the autonomous command (example)
        if (autonomousCommand != null) autonomousCommand.start();
    }

    /**
     * This function is called periodically during autonomous
     */
    public void autonomousPeriodic() {
        Scheduler.getInstance().run();
        autonomousLoopCounter++;
        if(autonomousLoopCounter<20){
    		pickUpSolenoid.set(DoubleSolenoid.Value.kForward);
        }
        ///runs programming according to button pressed in driver view
		
        if(numberOfInitButtonsPressed==1){
        	switch(buttonInitPressed){
        	///autonomous mode for portcullis
        	case 1:
        		switch(portcullisAutoState){
        		case FORWARD1:
        			//robot move forward
        			if(obstacleSonar.getRangeMM()<=25){
        				portcullisAutoCounter=0;
        				portcullisAutoState=PortcullisAutoStates_t.STOP;
        			}
        			break;
        		case STOP:
        			//stop robot.
        			if(portcullisAutoCounter++>10){
        				portcullisAutoCounter=0;
        				portcullisAutoState=PortcullisAutoStates_t.LIFT;
        			}
        			break;
        		case LIFT:
        			//lift gate
        			if(portcullisAutoCounter++>25){
        				portcullisAutoCounter=0;
        				portcullisAutoState=PortcullisAutoStates_t.FORWARD2;
        			}
        			break;
        			
        		case FORWARD2:
        			//go forard again
        			if(portcullisAutoCounter++>100){
        				portcullisAutoCounter=0;
        				portcullisAutoState=PortcullisAutoStates_t.IDLE;
        			}
        		}
        		break;
        	case 2:
        		/////if buttton 2 is pressed etc.....
        		break;
        	case 3:
        		break;
        	case 4:
        		break;
        	default:
        		////runs the default code
        		System.out.println("no button or multiple buttons selected");
        		break;
        	}
        		
        	
        }
        
              
    }

	public void teleopInit() {
    	shooterTimer=0;
    	shooterState=ShooterStates_t.IDLE;

		// This makes sure that the autonomous stops running when
        // teleop starts running. If you want the autonomous to 
        // continue until interrupted by another command, remove
        // this line or comment it out.
        if (autonomousCommand != null) autonomousCommand.cancel();
    }

    /**
     * This function is called periodically during operator control
     */
    public void teleopPeriodic() {
        Scheduler.getInstance().run();
        ///gets values from joystick on the controller
        leftXAxis=controller.getRawAxis(0);
    	leftYAxis=controller.getRawAxis(1)*-1;
    	rightXAxis=controller.getRawAxis(4);
    	rightYAxis=controller.getRawAxis(5)*-1;
        ///updates buttons values by asking for if is edge
        for(int i=0;i<Buttons.length;i++){
        	Edges[i]=Buttons[i].isEdge();
        }
        ///drives the robot
        myRobot.arcadeDrive(rightYAxis,rightXAxis*-1,true); /// Does a thing.
        
      
      
        /////does the shooter "motion" for robot
        /*
        switch(shooterState){
        case IDLE:
        	if(controller.getRawButton(A)){
        		shooterState=ShooterStates_t.FORWARD;
        		shooterTimer=0;
        	}
        
        case FORWARD:
        	shooter1.set(1);
        	shooter2.set(1);
        	if(shooterTimer++>25){
        		shooterState=ShooterStates_t.IDLE;
        		shooter1.set(0);
        		shooter2.set(0);
        		shooterTimer=0;
        	}
        	break;
        }	
        */
    
        ///contrrols the solenoids that lift the robot off the ground CHNAGE THE GETRAWBUTTON TO BUTTON CLASS
       
        if(controller.getRawButton(X)){
        	pickUpSolenoid.set(DoubleSolenoid.Value.kForward);
        }
        if(controller.getRawButton(Y)){
        	pickUpSolenoid.set(DoubleSolenoid.Value.kReverse);
        }
        ////moves camerservo up and down
    	double cameraMovement=0;
    	double cameraPosition=cameraServo.getPosition();

        if(controller.getRawButton(LTRIGGER) && !controller.getRawButton(RTRIGGER)){
        	cameraMovement+=.05;
        	cameraPosition+=cameraMovement;
        	cameraServo.setPosition(cameraPosition);
        }
        if(!controller.getRawButton(LTRIGGER) && controller.getRawButton(RTRIGGER)){
        	cameraMovement-=.05;
        	cameraPosition+=cameraMovement;
        	cameraServo.setPosition(cameraPosition);
        }
        //prints out sensor values in console, CHANGE LATER TO DISPLAY IN DRIVER STATION
        ///double sliderData=SmartDashboard.getNumber("DB/Slider 0", 0.0); 
    	SmartDashboard.putString("DB/String 1", "ballSonar: "+String.valueOf(ballSonar.getRangeMM()));
    	SmartDashboard.putString("DB/String 2", "obstacleSonar: "+String.valueOf(obstacleSonar.getRangeMM()));
    	if(controller.getRawButton(B)){
    		spinbar.set(-.8);
    	}
    	else{
    		spinbar.set(0);
    	}	
    	if(controller.getRawButton(A)){
    		shooter1.set(1);
    		shooter2.set(1);
    	}
    	else{
    		shooter1.set(0);
    		shooter2.set(0);
    	}	
    }
    
    
    /**
     * This function is called periodically during test mode
     */
    public void testPeriodic() {
        LiveWindow.run();
    }
    
}
