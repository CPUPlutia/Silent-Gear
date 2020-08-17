/*
 * Silent Gear -- PartPositions
 * Copyright (C) 2018 SilentChaos512
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 3
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.silentchaos512.gear.gear.part;

import net.silentchaos512.gear.api.part.IPartPosition;

// FIXME: Allow mods to register new part positions
public enum PartPositions implements IPartPosition {
    ANY("head", "any", false),
    ARMOR("main", "main", false),
    ROD("rod", "rod", true),
    GRIP("grip", "grip", true),
    HEAD("head", "head", true),
    GUARD("guard", "guard", true),
    COATING("coating", "coating", true),
    HIGHLIGHT("", "highlight", true),
    TIP("tip", "tip", true),
    BOWSTRING("bowstring", "bowstring", true),
    FLETCHING("fletching", "fletching", true),
    BINDING("binding", "binding", true);

    private final String texturePrefix;
    private final String modelKey;

    PartPositions(String texture, String model, boolean isRenderLayer) {
        this.texturePrefix = texture;
        this.modelKey = model;

        if (isRenderLayer) RENDER_LAYERS.add(this);
    }

}
