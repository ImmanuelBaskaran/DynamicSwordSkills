/**
    Copyright (C) <2017> <coolAlias>

    This file is part of coolAlias' Dynamic Sword Skills Minecraft Mod; as such,
    you can redistribute it and/or modify it under the terms of the GNU
    General Public License as published by the Free Software Foundation,
    either version 3 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package dynamicswordskills.loot.conditions;

import java.util.Random;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import dynamicswordskills.loot.functions.SkillFunction;
import dynamicswordskills.ref.ModInfo;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.conditions.LootCondition;

/**
 * 
 * Condition guarantees at least one skill is enabled; use as sanity
 * check with skill functions.
 *
 */
public class SkillCondition implements LootCondition
{
	public SkillCondition() {}

	@Override
	public boolean testCondition(Random rand, LootContext context) {
		return !SkillFunction.SKILL_IDS.isEmpty();
	}

	public static class Serializer extends LootCondition.Serializer<SkillCondition>
	{
		public Serializer() {
			super(new ResourceLocation(ModInfo.ID, "skill_condition"), SkillCondition.class);
		}
		@Override
		public void serialize(JsonObject json, SkillCondition instance, JsonSerializationContext context) {
			// nothing to serialize
		}
		@Override
		public SkillCondition deserialize(JsonObject json, JsonDeserializationContext context) {
			return new SkillCondition();
		}
	}
}
