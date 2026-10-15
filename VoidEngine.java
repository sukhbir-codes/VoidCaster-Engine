import javax.swing.*;
import java.awt.*;
import java.awt.image.*;

public final class VoidEngine extends JFrame implements Runnable {
    public static final int WIDTH = 640;
    public static final int HEIGHT = 480;

    private final BufferedImage screenBuffer;
    private final int[] pixels;

    private double posX = 3.5;
    private double posY = 3.5;

    private double dirX = 1.0;
    private double dirY = 0.0;

    private boolean running = false;

    private boolean forward, backward, turnLeft, turnRight;
    private final double moveSpeed = 3.0;
    private final double rotSpeed = 2.0;

    private static final int[][] MAP = {
        {1,1,1,1,1,1,1,1},
        {1,0,0,0,0,0,0,1},
        {1,0,1,0,0,1,0,1},
        {1,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,1},
        {1,1,1,1,1,1,1,1},
    };
    public VoidEngine() {
        setTitle("VoidCaster");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        screenBuffer = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

        pixels = ((DataBufferInt) screenBuffer.getRaster().getDataBuffer()).getData();

        int half = (WIDTH * HEIGHT)/2;
        for(int i=0; i<half;i++)
            pixels[i] = 0x00FF00;
        for(int i=half;i<WIDTH * HEIGHT;i++)
            pixels[i]= 0x0000FF;

        JPanel viewport = new JPanel(){
            @Override 
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(screenBuffer, 0, 0, getWidth(), getHeight(), null);
            }
        };
        viewport.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        add(viewport);
        pack();
        setLocationRelativeTo(null);

        addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                switch (e.getKeyCode()) {
                    case java.awt.event.KeyEvent.VK_W, java.awt.event.KeyEvent.VK_UP -> forward = true;
                    case java.awt.event.KeyEvent.VK_S, java.awt.event.KeyEvent.VK_DOWN -> backward = true;
                    case java.awt.event.KeyEvent.VK_A, java.awt.event.KeyEvent.VK_LEFT -> turnLeft = true;
                    case java.awt.event.KeyEvent.VK_D, java.awt.event.KeyEvent.VK_RIGHT -> turnRight = true;
                }
            }

            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                switch (e.getKeyCode()) {
                    case java.awt.event.KeyEvent.VK_W, java.awt.event.KeyEvent.VK_UP -> forward = false;
                    case java.awt.event.KeyEvent.VK_S, java.awt.event.KeyEvent.VK_DOWN -> backward = false;
                    case java.awt.event.KeyEvent.VK_A, java.awt.event.KeyEvent.VK_LEFT -> turnLeft = false;
                    case java.awt.event.KeyEvent.VK_D, java.awt.event.KeyEvent.VK_RIGHT -> turnRight = false;
                }
            }
        });
        setFocusable(true);
requestFocusInWindow();
    }
    public synchronized void start(){
        running=true;
        new Thread(this, "EngineThread").start();
    }

    @Override public void run() {
        long lastTime = System.nanoTime();
        final double nsPerTick = 1_000_000_000.0/60.0;
        double delta = 0;

        while (running) {
            long now = System.nanoTime();
            delta+= (now - lastTime) / nsPerTick;
            lastTime = now;

            while(delta>=1.0){
                update(1.0/60.0);
                delta--;
            }

            render();
            repaint();

            try{
                Thread.sleep(1);
            }catch (InterruptedException e){
                Thread.currentThread().interrupt();
            }
        }
    }

    private void drawRect(int rx, int ry, int rw, int rh, int color){
        for(int y=ry; y<ry+rh; y++){
            if(y<0 || y>=HEIGHT) continue;
            int rowOffset = y*WIDTH;
            for(int x=rx; x<rx+rw;x++){
                if(x>=0 &&x<WIDTH){
                    pixels[rowOffset+x]=color;
                }
            } 
        }
    }
    
    public void render(){
        int total = WIDTH * HEIGHT;
        int half = total / 2;
        for (int i=0; i<half;i++ )pixels[i]=0xFF141418;
        for(int i=half;i<total;i++)pixels[i]=0xFF08080A;

        int tileSize=16;
        for(int r =0;r<MAP.length;r++){
            for(int c=0;c<MAP[r].length;c++){
                int tileColor =(MAP[r][c] ==1) ? 0xFF888888 :0xFF222222;
                drawRect(c*tileSize, r*tileSize,tileSize-1,tileSize-1, tileColor );
            }
        }

        int playerPx=(int)(posX*tileSize);
        int playerPy=(int)(posY*tileSize);
        drawRect(playerPx-2,playerPy-2,5,5,0xFF00E5FF);

        // Draw player direction pointer (12-pixel line)
for (int step = 0; step < 12; step++) {
    int lx = (int) (playerPx + dirX * step);
    int ly = (int) (playerPy + dirY * step);
    if (lx >= 0 && lx < WIDTH && ly >= 0 && ly < HEIGHT) {
        pixels[ly * WIDTH + lx] = 0xFFFFFFFF; // White pointer
    }
}


    }
    private void update(double dt) {
    // 1. Forward / Backward translation with basic wall collision
    if (forward) {
        double nextX = posX + dirX * moveSpeed * dt;
        double nextY = posY + dirY * moveSpeed * dt;
        if (MAP[(int) posY][(int) nextX] == 0) posX = nextX;
        if (MAP[(int) nextY][(int) posX] == 0) posY = nextY;
    }
    if (backward) {
        double nextX = posX - dirX * moveSpeed * dt;
        double nextY = posY - dirY * moveSpeed * dt;
        if (MAP[(int) posY][(int) nextX] == 0) posX = nextX;
        if (MAP[(int) nextY][(int) posX] == 0) posY = nextY;
    }

    // 2. Turning (Rotating the Direction Vector)
    if (turnLeft) rotate(-rotSpeed * dt);
    if (turnRight) rotate(rotSpeed * dt);
}

private void rotate(double angle) {
    // Standard 2D Rotation Matrix:
    // [ x' ] = [ cos(a)  -sin(a) ] [ x ]
    // [ y' ] = [ sin(a)   cos(a) ] [ y ]
    double cosA = Math.cos(angle);
    double sinA = Math.sin(angle);

    double oldDirX = dirX;
    dirX = dirX * cosA - dirY * sinA;
    dirY = oldDirX * sinA + dirY * cosA;

    // Normalization to prevent floating-point drift over time
    double len = Math.sqrt(dirX * dirX + dirY * dirY);
    dirX /= len;
    dirY /= len;
}
    public static void main(String args[]){
        javax.swing.SwingUtilities.invokeLater(() -> {
            VoidEngine engine = new VoidEngine();
            engine.setVisible(true);
            engine.start();
        });
    }
}