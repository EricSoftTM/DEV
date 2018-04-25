/*    */ package client;
/*    */ 
/*    */ import java.io.Serializable;
/*    */ 
/*    */ public class CardData
/*    */   implements Serializable
/*    */ {
/*    */   private static final long serialVersionUID = 2550550428979893978L;
/*    */   public int cid;
/*    */   public short job;
/*    */   public short level;
/*    */ 
/*    */   public CardData(int cid, short level, short job)
/*    */   {
/* 33 */     this.cid = cid;
/* 34 */     this.level = level;
/* 35 */     this.job = job;
/*    */   }
/*    */ 
/*    */   public String toString()
/*    */   {
/* 40 */     return "CID: " + this.cid + ", Job: " + this.job + ", Level: " + this.level;
/*    */   }
/*    */ }

/* Location:           C:\Users\DKB\Desktop\ololol\v113.jar
 * Qualified Name:     client.CardData
 * JD-Core Version:    0.6.0
 */