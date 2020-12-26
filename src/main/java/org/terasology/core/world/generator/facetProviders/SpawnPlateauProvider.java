// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.core.world.generator.facetProviders;

import org.terasology.math.Region3i;
import org.terasology.math.TeraMath;
import org.terasology.math.geom.BaseVector2i;
import org.terasology.math.geom.ImmutableVector2i;
import org.terasology.math.geom.Rect2i;
import org.terasology.world.block.BlockRegion;
import org.terasology.world.generation.Facet;
import org.terasology.world.generation.FacetBorder;
import org.terasology.world.generation.FacetProvider;
import org.terasology.world.generation.GeneratingRegion;
import org.terasology.world.generation.Requires;
import org.terasology.world.generation.Updates;
import org.terasology.world.generation.facets.ElevationFacet;
import org.terasology.world.generation.facets.SeaLevelFacet;


/**
 * Flattens the surface in a circular area around a given coordinate.
 * <p>
 * The area outside this area will be adjusted up to a fixed radius of {@link SpawnPlateauProvider#OUTER_RADIUS} to
 * generate a smooth embedding with the {@link ElevationFacet}. It is guaranteed that the plateau is above the sea level
 * as defined by {@link SeaLevelFacet}.
 * <pre>
 *           inner rad.
 *           __________
 *          /          \
 *         /            \
 *    ~~~~~  outer rad.  ~~~~~
 * </pre>
 */
@Requires(@Facet(SeaLevelFacet.class))
@Updates(@Facet(value = ElevationFacet.class, border = @FacetBorder(sides = SpawnPlateauProvider.OUTER_RADIUS)))
public class SpawnPlateauProvider implements FacetProvider {

    public static final int OUTER_RADIUS = 16;
    public static final int OUTER_RADIUS_SQUARED = OUTER_RADIUS * OUTER_RADIUS;
    public static final int INNER_RADIUS = 4;

    private final ImmutableVector2i centerPos;

    /**
     * @param center the center of the circle-shaped plateau
     */
    public SpawnPlateauProvider(BaseVector2i center) {
        this.centerPos = ImmutableVector2i.createOrUse(center);
    }

    @Override
    public void process(GeneratingRegion region) {
        BlockRegion reg = region.getRegion();
        Rect2i rc = Rect2i.createFromMinAndMax(reg.minX(), reg.minZ(), reg.maxX(), reg.maxZ());

        if (rc.distanceSquared(centerPos.x(), centerPos.y()) <= OUTER_RADIUS_SQUARED) {
            ElevationFacet facet = region.getRegionFacet(ElevationFacet.class);
            SeaLevelFacet seaLevel = region.getRegionFacet(SeaLevelFacet.class);

            float targetHeight = Math.max(facet.getWorld(centerPos), seaLevel.getSeaLevel() + 3);

            // update the surface height
            for (BaseVector2i pos : facet.getWorldRegion().contents()) {
                float originalValue = facet.getWorld(pos);
                int distSq = pos.distanceSquared(centerPos);

                if (distSq <= INNER_RADIUS * INNER_RADIUS) {
                    facet.setWorld(pos, targetHeight);
                } else if (distSq <= OUTER_RADIUS_SQUARED) {
                    double dist = pos.distance(centerPos) - INNER_RADIUS;
                    float norm = (float) dist / (OUTER_RADIUS - INNER_RADIUS);
                    facet.setWorld(pos, TeraMath.lerp(targetHeight, originalValue, norm));
                }
            }
        }
    }
}
