package org.usfirst.frc.team5968.robot;

import edu.wpi.first.wpilibj.CounterBase.EncodingType;
import edu.wpi.first.wpilibj.interfaces.Gyro;

import edu.wpi.first.wpilibj.ADXRS450_Gyro;
import edu.wpi.first.wpilibj.Encoder;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.Victor;

public class Drive {
	
	private double leftRate;
	private double rightRate;
	
	private long nanotimeOld;
	
	private Victor leftMotor;
	private Victor rightMotor;
	
	private Encoder leftEncoder;
	private Encoder rightEncoder;
	private double leftEncoderOld;
	private double rightEncoderOld;
	
	private double rotationsNeeded, diameter;
	private double leftDistance;
	private double rightDistance;
	private double c;
	
	private Gyro gyro;
	
	public Drive(){

		leftMotor = new Victor(PortMap.leftMotor);
		rightMotor = new Victor(PortMap.rightMotor);
		
		leftEncoder = new Encoder(PortMap.leftEncoderOne, PortMap.leftEncoderTwo, false, EncodingType.k1X);
		rightEncoder = new Encoder(PortMap.rightEncoderOne, PortMap.rightEncoderTwo, false, EncodingType.k1X);
		
		
		nanotimeOld = System.nanoTime();
		leftEncoderOld = leftEncoder.get();
		rightEncoderOld = rightEncoder.get();
		
		gyro = new ADXRS450_Gyro();
		
	}
	
	
	public boolean driveDistance(double driveDistance){
		
		leftMotor.set(0.25);
		rightMotor.set(-0.25);
		
		leftRate = ((leftEncoder.get() - leftEncoderOld) * PortMap.countsPerRevolution) / ((System.nanoTime() - nanotimeOld) * 60 * Math.pow(10, 9));
		rightRate = ((rightEncoder.get() - rightEncoderOld) * PortMap.countsPerRevolution) / ((System.nanoTime() - nanotimeOld) * 60 * Math.pow(10, 9));
		
		nanotimeOld = System.nanoTime();
		
		leftDistance = leftEncoder.get() * PortMap.countsPerRevolution * 7.65 * Math.PI;
		rightDistance = rightEncoder.get() * PortMap.countsPerRevolution * 7.65 * Math.PI;
		
		if(leftRate != rightRate)
		{
			fixDirection(leftRate, rightRate, false);
		}
		if(Math.abs(leftDistance - driveDistance) <= .1 || Math.abs(rightDistance - driveDistance) <= .1){
			
			leftMotor.set(0);
			rightMotor.set(0);
			
			leftEncoder.reset();
			rightEncoder.reset();
			
			return true;
		}
		
		else if(Timer.getMatchTime() >= 10)
		{
			leftMotor.set(0);
			rightMotor.set(0);
			return false;
		}
		
		nanotimeOld = System.nanoTime();
		leftEncoderOld = leftEncoder.get();
		rightEncoderOld = rightEncoder.get();
		
		return false;
	}//end of some method
	
	public enum CrossingStates {
		ENTERED, LEAVING, CROSSED, MISC_RESPONSE
	}
	
	private CrossingStates crossingState = null;
	private int flatSamples = 0;
	
	public CrossingStates driveAcrossDefense(){

		leftMotor.set(0.25);
		rightMotor.set(-0.25);
		
		leftRate = ((leftEncoder.get() - leftEncoderOld) * PortMap.countsPerRevolution) / ((System.nanoTime() - nanotimeOld) * 60 * Math.pow(10, 9));
		rightRate = ((rightEncoder.get() - rightEncoderOld) * PortMap.countsPerRevolution) / ((System.nanoTime() - nanotimeOld) * 60 * Math.pow(10, 9));
		
		leftEncoderOld = leftEncoder.get();
		rightEncoderOld = rightEncoder.get();
		nanotimeOld = System.nanoTime();
		
		if(leftRate != rightRate)
		{
			fixDirection(leftRate, rightRate, false);
		}
		
		if(gyro.getAngle() > 0 && crossingState == null)
		{
			crossingState = CrossingStates.ENTERED;
			flatSamples = 0;
			return CrossingStates.ENTERED;
			
		}
		
		if(gyro.getAngle() < 0 && crossingState == CrossingStates.ENTERED || crossingState == CrossingStates.LEAVING)
		{
			crossingState = CrossingStates.LEAVING;
			flatSamples = 0;
			return CrossingStates.LEAVING;
		}
		
		if(Math.abs(gyro.getAngle()) <= .01 && crossingState == CrossingStates.ENTERED)
		{
			flatSamples++;
			
			if(flatSamples >= 100)
			{
				return CrossingStates.CROSSED;
			}
			
			else{
				return CrossingStates.ENTERED;
			}
		}
		
		else
		{
			flatSamples = 0;
			crossingState = CrossingStates.ENTERED;
			return CrossingStates.ENTERED;
		}

	}//end of method
	
	
	public void scootUp(double dist)
	{
		rotationsNeeded = dist/(diameter * Math.PI);
		rotationsNeeded = (int)Math.ceil(rotationsNeeded) * 512;
		while (true)
		{
			if (leftEncoder.get() != rotationsNeeded + leftEncoder.get())
			{
				leftMotor.set(.1);
				rightMotor.set(-.1);
			}
			else
			{
				leftMotor.set(0);
				rightMotor.set(0);
				return;
			}
		}
	}//end of scootUp
	
	public void scootBack(double dist)
	{
		rotationsNeeded = dist/(diameter * Math.PI);
		rotationsNeeded = (int)Math.ceil(rotationsNeeded) * PortMap.countsPerRevolution;
		while (true)
		{
			if (leftEncoder.get() != leftEncoder.get() - rotationsNeeded)
			{
				leftMotor.set(-.1);
				rightMotor.set(.1);
			}
			else
			{
				leftMotor.set(0);
				rightMotor.set(0);
				return;
			}
		}
	}
	public double fixDirection(double leftRate, double rightRate, boolean turning)
	{
			
			if(turning)
			{
				if(leftRate < rightRate)
				{
					rightMotor.set(leftRate / 67702.5);
				}
			
				if(rightRate < leftRate)
				{				
					leftMotor.set(rightRate / 67702.5);
				}
			}
			
			if(!turning)
			{
				if(leftRate < rightRate)
				{
					rightMotor.set(-1 * leftRate / 67702.5);
				}
			
				if(rightRate < leftRate)
				{				
					leftMotor.set(rightRate / 67702.5);				
				}
			}
		return 0;
	}
	
	
	public boolean turn(double turnAngle, String direction) {
		
		leftDistance = leftEncoder.get() * PortMap.countsPerRevolution * 7.65 * Math.PI;
		rightDistance = rightEncoder.get() * PortMap.countsPerRevolution * 7.65 * Math.PI;
			
		leftRate = ((leftEncoder.get() - leftEncoderOld) * PortMap.countsPerRevolution) / ((System.nanoTime() - nanotimeOld) * 60 * Math.pow(10, 9));
		rightRate = ((rightEncoder.get() - rightEncoderOld) * PortMap.countsPerRevolution) / ((System.nanoTime() - nanotimeOld) * 60 * Math.pow(10, 9));
		
		c = 22.4 * Math.PI * (turnAngle / 360);
			
		if(leftDistance - c >= .1 || rightDistance - c >= .1)
		{
				
			if(direction.equals("counterclockwise"))
			{
				leftMotor.set(.5);
				rightMotor.set(.5);
				
			}
			else if(direction.equals("clockwise"))
			{
				leftMotor.set(-.5);
				rightMotor.set(-.5);
					
			}
			fixDirection(leftRate, rightRate, true);
				
			nanotimeOld = System.nanoTime();
			leftEncoderOld = leftEncoder.get();
			rightEncoderOld = rightEncoder.get();
		
		}
		if(leftDistance - c <= .1 || rightDistance - c <= .1)
		{
			leftMotor.set(0);
			rightMotor.set(0);
			
			return true;
		}
		return false;
	}
	/*public void scootAngle(double angle)
	{
		if (angle < angle_MIN)
		{
			
		}
	}*/
	public void humanDrive(double leftSpeed, double rightSpeed)
	{
		leftMotor.set(leftSpeed);
		rightMotor.set(rightSpeed);
	}
}
