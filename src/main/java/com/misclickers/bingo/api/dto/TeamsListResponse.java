package com.misclickers.bingo.api.dto;

import java.util.List;

/** Response shape from GET /boards/:boardId/teams. */
public class TeamsListResponse {
    public List<TeamSummary> teams;

    public static final class TeamSummary {
        public String id;
        public String name;
    }
}
