package com.zhenq.core.asset.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * unDraw 插画素材
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IllustrationAsset {

    private String title;

    private String pageUrl;

    private String svgUrl;
}
