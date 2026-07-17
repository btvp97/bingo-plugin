package com.misclickers.bingo.api.dto;

/** Mirrors backend src/scoring.ts's ScoreBreakdown return shape exactly. */
public class ScoreBreakdown {
    public int tilePoints;
    public int repeatBonusPoints;
    public int lineBonusPoints;
    public int blackoutBonusPoints;
    public int total;
    public int completedCount;
}
