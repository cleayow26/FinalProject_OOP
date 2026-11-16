package com.neonpulse.backend.run;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ScorePolicyTest {

    @Test
    void perfectOnlyScoresOneHundredEach() {
        assertThat(ScorePolicy.compute(3, 0, 0, 60_000L, false)).isEqualTo(300);
    }

    @Test
    void goodOnlyScoresFiftyEach() {
        assertThat(ScorePolicy.compute(0, 2, 0, 60_000L, false)).isEqualTo(100);
    }

    @Test
    void completedAddsFiveHundred() {
        assertThat(ScorePolicy.compute(0, 0, 0, 1L, true)).isEqualTo(500);
    }

    @Test
    void bothJudgementsAndCompletedAddUp() {
        assertThat(ScorePolicy.compute(3, 2, 1, 60_000L, true)).isEqualTo(900);
    }

    @Test
    void zeroValuesScoreZero() {
        assertThat(ScorePolicy.compute(0, 0, 0, 1L, false)).isZero();
    }

    @Test
    void missAndDurationDoNotAffectScore() {
        int baseline = ScorePolicy.compute(3, 2, 0, 1L, true);

        assertThat(ScorePolicy.compute(3, 2, 99, 9_999_999L, true)).isEqualTo(baseline);
    }
}
