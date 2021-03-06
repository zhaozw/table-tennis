package com.funprog.tabletennis;

import java.util.Iterator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

/**
* The GameScreen class is the screen where the main game play
* elements occur. It brings together the ball, paddle, and table
* while checking for input and rendering output.
*/
public class GameScreen implements Screen{
	private static final float WORLD_WIDTH = 10;
	private static final float WORLD_HEIGHT = 6;
	private static final float DPAD_MOVEMENT_SPEED = 0.5f;
	
	TableTennis game;
	
	OrthographicCamera camera;
	World world;
	Box2DDebugRenderer debugRenderer;
	SpriteBatch spriteBatch;
	
	Ball ball;
	Table table;
	Paddle leftPad;
	ComputerPaddle rightPad;
	
	Vector3 touchPos;
	
	ControlTool resetBall;
	RotateTool rotate;
	MovementTool movement;
	
	Texture background;
	Texture tableTexture;
	
	// Used in getInput() to determine if user used movement tool to move paddle
	private boolean touchedMovementTool;
	
	/**
	* Constructor that initializes the variables and takes
	* the game as an argument to have the ability to change
	* screens.
	*
	* @param game The game that maintains the screens.
	*/
	GameScreen(TableTennis game) {
		this.game = game;
	}
	
	/**
	 * Checks for input and renders the world.
	 * @param delta
	 */
	@Override
	public void render(float delta) {
		// Clear screen with black
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		
		getInput();
		rightPad.defendPosition(ball);
		
		leftPad.constrainTo(new Rectangle(0, 0, WORLD_WIDTH / 2, WORLD_HEIGHT));
		rightPad.constrainTo(new Rectangle(WORLD_WIDTH * 2 / 3, 0, 
				WORLD_WIDTH / 3, WORLD_HEIGHT * 4 / 5));
		
		// Go to the next step and render the world
		world.step(delta, 8, 3);
		
		// Draw all the sprites
		spriteBatch.setProjectionMatrix(camera.combined);
		spriteBatch.begin();
		
		// Draw the background
		spriteBatch.draw(background, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
		// Draw the table
		spriteBatch.draw(tableTexture, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
		
		// Draw the tools
		resetBall.draw(spriteBatch);
		rotate.draw(spriteBatch);
		movement.draw(spriteBatch);
		
		// Draw the sprites of all the bodies
		Iterator<Body> bi = world.getBodies();
        
		while (bi.hasNext()){
		    Body b = bi.next();
		    
		    Sprite spr = (Sprite) b.getUserData();

		    if (spr != null) {
		        spr.setPosition(b.getPosition().x - spr.getOriginX(),
		        		b.getPosition().y - spr.getOriginY());
		        spr.setRotation(MathUtils.radiansToDegrees * b.getAngle());
		        spr.draw(spriteBatch);
		    }
		}
		
		spriteBatch.end();
		
		// Draw the box2d bodies for debugging
		debugRenderer.render(world, camera.combined);
	}
	
	@Override
	public void resize(int width, int height) {
	}
	
	@Override
	public void show() {
		camera = new OrthographicCamera();
		camera.setToOrtho(false, WORLD_WIDTH, WORLD_HEIGHT);
		world = new World(new Vector2(0, -10), true); // Create world with gravity
		debugRenderer = new Box2DDebugRenderer();
		spriteBatch = new SpriteBatch();
		
		ball = new Ball(world, new Vector2(2, 4), new Texture(Gdx.files.internal("ball.png")));
		ball.stop(); // Don't let gravity affect the ball initially.
		
		table = new Table(world, new Vector2(5, 2.5f), 8, 0.1f);
		
		leftPad = new Paddle(world, new Vector2(1, 3.5f), 
				new Texture(Gdx.files.internal("paddle.png")));
		rightPad = new ComputerPaddle(world, new Vector2(9, 3.5f),
				new Texture(Gdx.files.internal("rightPaddle.png")), 8);
		
		resetBall = new ControlTool(new Texture(Gdx.files.internal("resetBall.png")), 
				new Rectangle(4.2f, 0.05f, 1.6f, 0.8f));
		rotate = new RotateTool(new Texture(Gdx.files.internal("rotate.png")),
				new Rectangle(8.35f, 0.05f, 1.6f, 1.6f));
		
		movement = new MovementTool(
				world, 
				new Texture(Gdx.files.internal("movement.png")),
				new Rectangle(0.05f, 0.05f, 2.5f, 2.5f),
				new Texture(Gdx.files.internal("movementBall.png")));		
	
		// Load texture for background image
		background = new Texture(Gdx.files.internal("background.png"));
		
		// Load texture for table
		tableTexture = new Texture(Gdx.files.internal("table.png"));
	}
	
	@Override
	public void hide() {
	}
	
	@Override
	public void pause() {
	}
	
	@Override
	public void resume() {
	}
	
	@Override
	public void dispose() {
	}
	
	/**
	 * Checks the touch screen and keyboard, then responds
	 */
	private void getInput() {
		touchedMovementTool = false;
		// Loop through each touch input
		for (int i = 0; Gdx.input.isTouched(i); i++) {
			touchPos = new Vector3(); // 3d vector used for camera.unproject
			touchPos.set(Gdx.input.getX(i), Gdx.input.getY(i), 0);
			camera.unproject(touchPos); // Make the touch input into camera coords
			
			if (resetBall.isTouched(touchPos.x, touchPos.y)) {
				// Move the ball to its starting position and stop it
				ball.setPosition(2, 4);
				ball.stop();
			} else if (rotate.isTouched(touchPos.x, touchPos.y)) {
				// Update the rotation image
				rotate.updateTouch(touchPos.x, touchPos.y);
				
				// Sync the paddle with the rotation tool
				leftPad.setRotation(rotate.getRotation());
			} else if (movement.isTouched(touchPos.x, touchPos.y)) {
				touchedMovementTool = true;
				// Move the paddle and update the movement image
				movement.updateTouch(touchPos.x, touchPos.y, leftPad, WORLD_WIDTH,
						WORLD_HEIGHT);
			} else if (!movement.isTouched(touchPos.x, touchPos.y)) {
				movement.repositionBall();		
			}
		}
		
		// If no input is detected
		if (!Gdx.input.isTouched()) {
			movement.repositionBall();
		}
		
		// Check for keyboard input and do not let the paddle rotate more
		// than PI / 4 radians
		if (Gdx.input.isKeyPressed(Keys.A)
				&& leftPad.getRotation() + 0.01f <= MathUtils.PI / 4) {
			leftPad.rotateCounterClockwise();
			
			// Sync the changing rotation with touch rotation
			rotate.setRotation(leftPad.getRotation());
		} else if (Gdx.input.isKeyPressed(Keys.D)
				&& leftPad.getRotation() - 0.01f >= -MathUtils.PI / 4) {
			leftPad.rotateClockwise();
			
			// Sync the changing rotation with touch rotation
			rotate.setRotation(leftPad.getRotation());
		} else {
			leftPad.stopRotating();
			
			// Ensure the paddle does not rotate more than PI / 4 radians
			if (leftPad.getRotation() > MathUtils.PI / 4) {
				leftPad.setRotation(MathUtils.PI / 4);
			}
			// Ensure the paddle does not rotate more than PI / 4 radians
			if (leftPad.getRotation() < -MathUtils.PI / 4) {
				leftPad.setRotation(-MathUtils.PI / 4);
			}
			
			if (!touchedMovementTool) {
				leftPad.stopMoving();
			}
		}
		
		// Handle DPAD input for paddle movement.
		if (Gdx.input.isKeyPressed(Keys.DPAD_UP)) {
			// Check if user is pressing right or left to determine
			// if paddle should go northwest or northeast
			if (Gdx.input.isKeyPressed(Keys.DPAD_RIGHT)) {
				leftPad.setVelocity(new Vector2(DPAD_MOVEMENT_SPEED,
						DPAD_MOVEMENT_SPEED));
			} else if (Gdx.input.isKeyPressed(Keys.DPAD_LEFT)) {
				leftPad.setVelocity(new Vector2(-DPAD_MOVEMENT_SPEED,
						DPAD_MOVEMENT_SPEED));
			} else {
				leftPad.setVelocity(new Vector2(0, DPAD_MOVEMENT_SPEED));
			}
		} else if (Gdx.input.isKeyPressed(Keys.DPAD_DOWN)) {
			// Check if user is pressing right or left to determine
			// if paddle should go southwest or southeast
			if (Gdx.input.isKeyPressed(Keys.DPAD_RIGHT)) {
				leftPad.setVelocity(new Vector2(DPAD_MOVEMENT_SPEED,
						-DPAD_MOVEMENT_SPEED));
			} else if (Gdx.input.isKeyPressed(Keys.DPAD_LEFT)) {
				leftPad.setVelocity(new Vector2(-DPAD_MOVEMENT_SPEED,
						-DPAD_MOVEMENT_SPEED));
			} else {
				leftPad.setVelocity(new Vector2(0, -DPAD_MOVEMENT_SPEED));
			}
		} else if (Gdx.input.isKeyPressed(Keys.DPAD_LEFT)) {
			leftPad.setVelocity(new Vector2(-DPAD_MOVEMENT_SPEED, 0));
		} else if (Gdx.input.isKeyPressed(Keys.DPAD_RIGHT)) {
			leftPad.setVelocity(new Vector2(DPAD_MOVEMENT_SPEED, 0));
		} 
	}
}