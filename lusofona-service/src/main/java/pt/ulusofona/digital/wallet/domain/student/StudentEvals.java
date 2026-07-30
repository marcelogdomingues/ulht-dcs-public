package pt.ulusofona.digital.wallet.domain.student;

import java.util.List;

public record StudentEvals(
        List<Evaluation> evaluationList,
        String errorCode
) {
    public record Evaluation(
            long evaluationDateTime,
            int curricularYear,
            List<Class> classList
    ) {
        public record Class(
                String className,
                List<EvaluationDetail> evaluationList,
                String curricularUnitName
        ) {
            public record EvaluationDetail(
                    String evaluationName,
                    int evaluationCode,
                    long evaluationDateTime,
                    String evaluationLocation,
                    String classDuration
            ) {
            }
        }
    }
}