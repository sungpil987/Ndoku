package com.sungpil987.ndoku;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.view.*;
import java.util.*;

public class MainActivity extends Activity {
    private GameView gameView;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(250,246,242));
        getWindow().setNavigationBarColor(Color.rgb(250,246,242));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        gameView = new GameView(this);
        setContentView(gameView);
    }
    @Override public void onBackPressed() { if (gameView != null && !gameView.goHome()) super.onBackPressed(); }

    static class GameView extends View {
        private final Paint p = new Paint(3);
        private final SharedPreferences prefs;
        private Bitmap adult, kitten;
        private final int bg=Color.rgb(250,246,242), ink=Color.rgb(111,75,79), muted=Color.rgb(153,116,113), coral=Color.rgb(229,105,110), orange=Color.rgb(246,151,36), white=Color.WHITE, line=Color.rgb(236,225,217);
        private final int[] regionColors={Color.rgb(248,194,211),Color.rgb(115,191,222),Color.rgb(187,218,155),Color.rgb(242,202,91)};
        private int[][] regions={{0,0,1,1},{0,0,1,1},{2,2,3,3},{2,2,3,3}};
        private int[][] cells=new int[4][4]; // 0 empty, 1 cat, 2 excluded
        private int[][] solution={{0,1,0,0},{0,0,0,1},{1,0,0,0},{0,0,1,0}};
        private boolean tutorial=false, won=false;
        private int screen=0, level, score, hearts=3, hints=3, mode=1;
        private String message="", messageKind="";
        private float boardX,boardY,cell,boardSize;
        private float startButtonTop, catModeY, xModeY, undoY, hintY, backX, backY;
        private int activeCat=0;
        private ArrayDeque<int[]> history=new ArrayDeque<>();
        private long messageUntil=0;
        GameView(Context c){ super(c); setLayerType(View.LAYER_TYPE_SOFTWARE,null); prefs=c.getSharedPreferences("nyangdoku",0); adult=BitmapFactory.decodeResource(getResources(),R.drawable.cat_adult); kitten=BitmapFactory.decodeResource(getResources(),R.drawable.cat_kitten); level=prefs.getInt("level",1); score=prefs.getInt("score",0); hints=prefs.getInt("hints",3); if(prefs.getBoolean("playing",false)){screen=1; restore();} setFocusable(true); }
        private void restore(){ for(int r=0;r<4;r++)for(int c=0;c<4;c++) cells[r][c]=prefs.getInt("c"+r+c,0); cells[0][1]=1; }
        private void save(){ prefs.edit().putInt("level",level).putInt("score",score).putInt("hints",hints).putBoolean("playing",screen==1).apply(); for(int r=0;r<4;r++)for(int c=0;c<4;c++)prefs.edit().putInt("c"+r+c,cells[r][c]).apply(); }
        private void resetBoard(){ cells=new int[4][4]; cells[0][1]=1; hearts=3; history.clear(); won=false; save(); }
        boolean goHome(){ if(screen==1){screen=0;save();invalidate();return true;} return false; }
        @Override protected void onDraw(Canvas c){ super.onDraw(c); c.drawColor(bg); float w=getWidth(),h=getHeight(); if(screen==0)drawHome(c,w,h); else if(screen==2)drawTutorial(c,w,h); else drawGame(c,w,h); if(messageUntil>System.currentTimeMillis()&&!message.isEmpty())drawToast(c,w,h); }
        private void text(Canvas c,String s,float x,float y,float size,int color,boolean bold,Paint.Align align){ p.setColor(color);p.setTextSize(size);p.setTypeface(bold?Typeface.create("sans-serif-rounded",Typeface.BOLD):Typeface.create("sans-serif-rounded",Typeface.NORMAL));p.setTextAlign(align);p.setStyle(Paint.Style.FILL); c.drawText(s,x,y,p); }
        private void round(Canvas c,float l,float t,float r,float b,float rad,int color){p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawRoundRect(l,t,r,b,rad,rad,p);}
        private void shadowCard(Canvas c,float l,float t,float r,float b,float rad){p.setColor(white);p.setShadowLayer(15,0,6,0x18000000);c.drawRoundRect(l,t,r,b,rad,rad,p);p.clearShadowLayer();}
        private void drawHome(Canvas c,float w,float h){
            float cx=w/2; text(c,"냥도쿠",cx,h*.17f,48,ink,true,Paint.Align.CENTER); text(c,"고양이와 함께 푸는 논리 퍼즐",cx,h*.215f,17,muted,false,Paint.Align.CENTER);
            float avatar= Math.min(w*.30f,128); drawCat(c,adult,cx-avatar*.63f,h*.34f,avatar,Color.rgb(249,214,223)); drawCat(c,kitten,cx+avatar*.63f,h*.34f,avatar,Color.rgb(216,237,247));
            shadowCard(c,w*.09f,h*.47f,w*.91f,h*.61f,22);text(c,"한 줄에 한 마리, 한 구역에 한 마리",cx,h*.515f,17,ink,true,Paint.Align.CENTER);text(c,"고양이끼리 대각선으로 붙지 않게 놓아봐!",cx,h*.555f,14,muted,false,Paint.Align.CENTER);
            startButtonTop=h*.72f; shadowCard(c,w*.13f,startButtonTop,w*.87f,startButtonTop+68,34);round(c,w*.13f,startButtonTop,w*.87f,startButtonTop+68,34,orange);text(c,"게임 시작",cx,startButtonTop+45,25,white,true,Paint.Align.CENTER);
            text(c,"레벨 "+level+"   ·   최고 점수 "+score,cx,h*.86f,15,muted,false,Paint.Align.CENTER);
        }
        private void drawTutorial(Canvas c,float w,float h){
            float cx=w/2; text(c,"냥도쿠 규칙",cx,h*.16f,34,ink,true,Paint.Align.CENTER);
            text(c,"이 네 가지만 기억하면 돼",cx,h*.205f,16,muted,false,Paint.Align.CENTER);
            float top=h*.29f; String[] rules={"가로줄마다 고양이 한 마리","세로줄마다 고양이 한 마리","같은 색 구역마다 한 마리","고양이끼리 대각선으로 붙지 않기"};
            for(int i=0;i<rules.length;i++){float y=top+i*70;shadowCard(c,w*.09f,y-28,w*.91f,y+25,18);text(c,""+(i+1),w*.16f,y+6,17,coral,true,Paint.Align.CENTER);text(c,rules[i],w*.23f,y+6,15,ink,true,Paint.Align.LEFT);}
            drawCat(c,adult,cx-46,h*.66f,78,Color.rgb(249,214,223));drawCat(c,kitten,cx+46,h*.66f,78,Color.rgb(216,237,247));
            float y=h*.76f;round(c,w*.13f,y,w*.87f,y+64,32,orange);text(c,"알겠어, 시작할게",cx,y+41,20,white,true,Paint.Align.CENTER);
        }
        private void drawGame(Canvas c,float w,float h){
            float margin=w*.065f; backX=margin+22;backY=48; circle(c,backX,backY,22,white);text(c,"‹",backX,backY+9,38,ink,true,Paint.Align.CENTER);
            text(c,"레벨",w*.47f,38,13,muted,false,Paint.Align.CENTER);text(c,""+level,w*.47f,64,23,ink,true,Paint.Align.CENTER);text(c,"점수",w*.70f,38,13,muted,false,Paint.Align.CENTER);text(c,""+score,w*.70f,64,23,ink,true,Paint.Align.CENTER);
            float gearX=w-margin-22;circle(c,gearX,backY,22,white);text(c,"⚙",gearX,backY+6,21,ink,true,Paint.Align.CENTER);
            text(c,"실수 기회",margin+8,114,13,muted,true,Paint.Align.LEFT);for(int i=0;i<3;i++)text(c,i<hearts?"♥":"♡",margin+22+i*25,148,23,i<hearts?coral:line,true,Paint.Align.CENTER);
            text(c,"색 구역마다 1마리 · 행/열마다 1마리 · 대각선으로 붙지 않기",w/2,188,13,ink,true,Paint.Align.CENTER);
            boardSize=Math.min(w-margin*2, h*.45f); boardX=(w-boardSize)/2;boardY=Math.max(215,h*.25f);cell=boardSize/4f;
            round(c,boardX-9,boardY-9,boardX+boardSize+9,boardY+boardSize+9,23,white);
            for(int r=0;r<4;r++)for(int col=0;col<4;col++){
                float x=boardX+col*cell,y=boardY+r*cell,g=5; round(c,x+g/2,y+g/2,x+cell-g/2,y+cell-g/2,13,regionColors[regions[r][col]]);
                if(cells[r][col]==1) drawCat(c,(r+col)%2==0?adult:kitten,x+cell/2f,y+cell/2f,cell*.76f,regionColors[regions[r][col]]);
                else if(cells[r][col]==2) text(c,"×",x+cell/2,y+cell*.73f,cell*.58f,white,true,Paint.Align.CENTER);
                else if(canPlace(r,col)) { p.setColor(0x557F6770);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawCircle(x+cell/2,y+cell/2,cell*.09f,p);p.setStyle(Paint.Style.FILL); }
            }
            float controlsY=boardY+boardSize+36; catModeY=controlsY+29;xModeY=controlsY+29;float left=w*.08f,right=w*.92f;
            button(c,left,controlsY,right,controlsY+57,mode==1?coral:white,mode==1?white:ink,"고양이 놓기");
            float buttonW=(right-left-14)/2;button(c,left,controlsY+70,left+buttonW,controlsY+124,mode==2?ink:white,mode==2?white:ink,"X 제외");button(c,left+buttonW+14,controlsY+70,right,controlsY+124,white,ink,"되돌리기");
            button(c,left,controlsY+138,right,controlsY+194,Color.rgb(255,239,205),ink,"힌트   ·   "+hints);
            if(controlsY+200>h-20){ /* smaller phones can scroll by scaling controls */ }
            if(won)drawWin(c,w,h);
        }
        private boolean canPlace(int r,int col){ if(cells[r][col]!=0)return false;for(int j=0;j<4;j++)if(cells[r][j]==1||cells[j][col]==1)return false;for(int rr=0;rr<4;rr++)for(int cc=0;cc<4;cc++)if(cells[rr][cc]==1){if(regions[rr][cc]==regions[r][col])return false;if(Math.abs(rr-r)<=1&&Math.abs(cc-col)<=1)return false;}return true; }
        private void drawCat(Canvas c,Bitmap b,float cx,float cy,float size,int color){ if(b==null){circle(c,cx,cy,size*.48f,color);return;} float left=cx-size/2,top=cy-size/2; p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawCircle(cx,cy,size*.48f,p);Path clip=new Path();clip.addCircle(cx,cy,size*.43f,Path.Direction.CW);int save=c.save();c.clipPath(clip);float bw=b.getWidth(),bh=b.getHeight(),side=Math.min(bw,bh);float sx=(bw-side)/2,sy=(bh-side)/2;Rect src=new Rect((int)sx,(int)sy,(int)(sx+side),(int)(sy+side));RectF dst=new RectF(left+size*.07f,top+size*.07f,left+size*.93f,top+size*.93f);p.setColor(Color.WHITE);p.setStyle(Paint.Style.FILL);c.drawBitmap(b,src,dst,p);c.restoreToCount(save);p.setColor(white);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(2,size*.035f));c.drawCircle(cx,cy,size*.44f,p);p.setStyle(Paint.Style.FILL); }
        private void circle(Canvas c,float x,float y,float r,int color){p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawCircle(x,y,r,p);}
        private void button(Canvas c,float l,float t,float r,float b,int color,int tc,String label){ round(c,l,t,r,b,18,color);text(c,label,(l+r)/2,(t+b)/2+6,17,tc,true,Paint.Align.CENTER); }
        private void drawToast(Canvas c,float w,float h){shadowCard(c,w*.12f,h*.80f,w*.88f,h*.87f,18);text(c,message,w/2,h*.845f,15,messageKind.equals("bad")?coral:ink,true,Paint.Align.CENTER);}
        private void drawWin(Canvas c,float w,float h){p.setColor(0x99000000);c.drawRect(0,0,w,h,p);shadowCard(c,w*.09f,h*.32f,w*.91f,h*.64f,28);text(c,"냥냥! 퍼즐 완성",w/2,h*.40f,27,ink,true,Paint.Align.CENTER);text(c,"고양이들이 안전한 자리를 찾았어",w/2,h*.45f,15,muted,false,Paint.Align.CENTER);button(c,w*.18f,h*.51f,w*.82f,h*.59f,orange,white,"다음 레벨");}
        private void say(String s,boolean bad){message=s;messageKind=bad?"bad":"ok";messageUntil=System.currentTimeMillis()+1600;invalidate();}
        private void markBad(){hearts--;say("앗, 규칙에 어긋났어!",true);if(hearts<=0){resetBoard();say("실수 기회를 다 썼어. 다시 해보자!",true);}}
        private void putCat(int r,int col){ if((r==0&&col==1)||cells[r][col]==1)return; if(!canPlace(r,col)){markBad();return;} if(solution[r][col]!=1){markBad();return;} history.push(new int[]{r,col,cells[r][col]});cells[r][col]=1;checkWin();save();invalidate(); }
        private void checkWin(){int count=0;for(int r=0;r<4;r++)for(int col=0;col<4;col++)if(cells[r][col]==1)count++;if(count==4){won=true;score+=100;save();invalidate();}}
        @Override public boolean onTouchEvent(android.view.MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_UP)return true;float x=e.getX(),y=e.getY(),w=getWidth(),h=getHeight();
            if(screen==0){if(y>startButtonTop-20){if(!prefs.getBoolean("tutorialSeen",false)){screen=2;}else{screen=1;resetBoard();}invalidate();return true;}return true;}
            if(screen==2){prefs.edit().putBoolean("tutorialSeen",true).apply();screen=1;resetBoard();invalidate();return true;}
            if(won){if(y>h*.49f&&y<h*.63f){level++;prefs.edit().putInt("level",level).apply();won=false;resetBoard();}invalidate();return true;}
            if(y<85&&x<getWidth()*.22f){screen=0;save();invalidate();return true;}
            if(y>=boardY&&y<boardY+boardSize&&x>=boardX&&x<boardX+boardSize){int r=(int)((y-boardY)/cell),col=(int)((x-boardX)/cell);if(mode==1)putCat(r,col);else{if(cells[r][col]==0){history.push(new int[]{r,col,0});cells[r][col]=2;}else if(cells[r][col]==2)cells[r][col]=0;save();invalidate();}return true;}
            float cy=boardY+boardSize+36; if(y>=cy&&y<=cy+57){mode=1;invalidate();return true;}if(y>=cy+70&&y<=cy+124){if(x>getWidth()*.53f){if(!history.isEmpty()){int[] a=history.pop();if(!(a[0]==0&&a[1]==1))cells[a[0]][a[1]]=a[2];save();invalidate();}}else{mode=2;invalidate();}return true;}if(y>=cy+138&&y<=cy+194){if(hints<=0){say("힌트를 모두 썼어",true);return true;}hints--;for(int r=0;r<4;r++)for(int col=0;col<4;col++)if(solution[r][col]==1&&cells[r][col]==0){history.push(new int[]{r,col,0});cells[r][col]=1;checkWin();save();invalidate();say("여기가 정답 칸이야!",false);return true;}return true;}
            return true;
        }
    }
}
