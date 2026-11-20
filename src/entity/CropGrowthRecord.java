package entity;

import java.time.LocalDateTime;

/** Entity: crop growth stage and NDVI (0..1) for a field, from a multispectral drone scan. */
public record CropGrowthRecord(int recordId, int fieldId, String growthStage, double ndvi, LocalDateTime recordedAt) { }
