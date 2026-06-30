package com.zhenq.core.asset.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pexels 照片素材
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoAsset {

    private String url;

    private String alt;

    private String photographer;
}
