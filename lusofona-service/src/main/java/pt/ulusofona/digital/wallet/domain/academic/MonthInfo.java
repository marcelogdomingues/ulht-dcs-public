package pt.ulusofona.digital.wallet.domain.academic;

public record MonthInfo(
        String monthName,
        int occupationMonth,
        long month1stDatTime // Use long for Unix timestamps
) {}