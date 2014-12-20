/**
    Copyright (C) <2014> <coolAlias>

    This file is part of coolAlias' Zelda Sword Skills Minecraft Mod; as such,
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

package dynamicswordskills;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.EnumToolMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import dynamicswordskills.api.ItemRandomSkill;
import dynamicswordskills.api.ItemSkillProvider;
import dynamicswordskills.entity.EntityLeapingBlow;
import dynamicswordskills.item.CombatSkillsTab;
import dynamicswordskills.item.ItemSkillOrb;
import dynamicswordskills.lib.Config;
import dynamicswordskills.lib.ModInfo;
import dynamicswordskills.network.CombatPacketHandler;
import dynamicswordskills.skills.SkillActive;
import dynamicswordskills.skills.SkillBase;

@Mod(modid = ModInfo.ID, version = ModInfo.VERSION)
@NetworkMod(clientSideRequired = true, serverSideRequired = true,
channels = {ModInfo.CHANNEL}, packetHandler = CombatPacketHandler.class)
public class DynamicSwordSkills
{
	@Instance(ModInfo.ID)
	public static DynamicSwordSkills instance;

	@SidedProxy(clientSide = ModInfo.CLIENT_PROXY, serverSide = ModInfo.COMMON_PROXY)
	public static CommonProxy proxy;

	/** Whether Battlegear2 mod is loaded */
	public static boolean isBG2Enabled;

	public static CreativeTabs tabSkills;

	public static Item skillOrb;

	public static List<Item> skillItems;

	/** Various randomized skill swords */
	public static Item
	skillWood,
	skillStone,
	skillGold,
	skillIron,
	skillDiamond;

	private boolean shouldLoad;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		shouldLoad = !Loader.isModLoaded("zeldaswordskills");
		isBG2Enabled = Loader.isModLoaded("battlegear2");
		if (shouldLoad) {
			Config.init(event);
			tabSkills = new CombatSkillsTab("dssTab");
			skillOrb = new ItemSkillOrb(Config.modItemIndex++).setUnlocalizedName("dss.skillorb");
			GameRegistry.registerItem(skillOrb, skillOrb.getUnlocalizedName().substring(5));
			if (Config.areCreativeSwordsEnabled()) {
				skillItems = new ArrayList<Item>(SkillBase.getNumSkills());
				for (SkillBase skill : SkillBase.getSkills()) {
					if (!(skill instanceof SkillActive)) {
						continue;
					}
					int level = (skill.getMaxLevel() == SkillBase.MAX_LEVEL ? Config.getSkillSwordLevel() : Config.getSkillSwordLevel() * 2);
					skillItems.add(new ItemSkillProvider(Config.modItemIndex++, EnumToolMaterial.IRON, skill, (byte) level).
							setUnlocalizedName("dss.skillitem" + skill.getId()).setCreativeTab(DynamicSwordSkills.tabSkills).setTextureName("iron_sword"));
					GameRegistry.registerItem(skillItems.get(skillItems.size() - 1), skillItems.get(skillItems.size() - 1).getUnlocalizedName().substring(5));
				}
			}
			if (Config.areRandomSwordsEnabled()) {
				skillWood = new ItemRandomSkill(Config.modItemIndex++, EnumToolMaterial.WOOD).setUnlocalizedName("dss.skillwood").setTextureName("wood_sword");
				GameRegistry.registerItem(skillWood, skillWood.getUnlocalizedName().substring(5));
				skillStone = new ItemRandomSkill(Config.modItemIndex++, EnumToolMaterial.STONE).setUnlocalizedName("dss.skillstone").setTextureName("stone_sword");
				GameRegistry.registerItem(skillStone, skillStone.getUnlocalizedName().substring(5));
				skillGold = new ItemRandomSkill(Config.modItemIndex++, EnumToolMaterial.GOLD).setUnlocalizedName("dss.skillgold").setTextureName("gold_sword");
				GameRegistry.registerItem(skillGold, skillGold.getUnlocalizedName().substring(5));
				skillIron = new ItemRandomSkill(Config.modItemIndex++, EnumToolMaterial.IRON).setUnlocalizedName("dss.skilliron").setTextureName("iron_sword");
				GameRegistry.registerItem(skillIron, skillIron.getUnlocalizedName().substring(5));
				skillDiamond = new ItemRandomSkill(Config.modItemIndex++, EnumToolMaterial.EMERALD).setUnlocalizedName("dss.skilldiamond").setTextureName("diamond_sword");
				GameRegistry.registerItem(skillDiamond, skillDiamond.getUnlocalizedName().substring(5));
			}
			EntityRegistry.registerModEntity(EntityLeapingBlow.class, "leapingblow", 0, this, 64, 10, true);
		}
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
		if (shouldLoad) {
			proxy.registerRenderers();
			MinecraftForge.EVENT_BUS.register(new DSSCombatEvents());
			DSSCombatEvents.initializeDrops();
			if (Config.getLootWeight() > 0) {
				registerSkillOrbLoot();
			}
			if (Config.areRandomSwordsEnabled()) {
				registerRandomSwordLoot();
			}
			NetworkRegistry.instance().registerGuiHandler(this, proxy);
		}
	}

	private void registerSkillOrbLoot() {
		for (SkillBase skill : SkillBase.getSkills()) {
			addLootToAll(new WeightedRandomChestContent(new ItemStack(skillOrb, 1, skill.getId()), 1, 1, Config.getLootWeight()), false);
		}
	}

	private void registerRandomSwordLoot() {
		addLootToAll(new WeightedRandomChestContent(new ItemStack(skillWood), 1, 1, 4), false);
		addLootToAll(new WeightedRandomChestContent(new ItemStack(skillStone), 1, 1, 3), false);
		addLootToAll(new WeightedRandomChestContent(new ItemStack(skillGold), 1, 1, 2), false);
		addLootToAll(new WeightedRandomChestContent(new ItemStack(skillIron), 1, 1, 2), false);
		addLootToAll(new WeightedRandomChestContent(new ItemStack(skillDiamond), 1, 1, 1), false);
	}

	/**
	 * Adds weighted chest contents to all ChestGenHooks, with possible exception of Bonus Chest
	 */
	private void addLootToAll(WeightedRandomChestContent loot, boolean bonus) {
		ChestGenHooks.getInfo(ChestGenHooks.MINESHAFT_CORRIDOR).addItem(loot);
		ChestGenHooks.getInfo(ChestGenHooks.PYRAMID_DESERT_CHEST).addItem(loot);
		ChestGenHooks.getInfo(ChestGenHooks.PYRAMID_JUNGLE_CHEST).addItem(loot);
		ChestGenHooks.getInfo(ChestGenHooks.STRONGHOLD_CORRIDOR).addItem(loot);
		ChestGenHooks.getInfo(ChestGenHooks.STRONGHOLD_LIBRARY).addItem(loot);
		ChestGenHooks.getInfo(ChestGenHooks.STRONGHOLD_CROSSING).addItem(loot);
		ChestGenHooks.getInfo(ChestGenHooks.VILLAGE_BLACKSMITH).addItem(loot);
		ChestGenHooks.getInfo(ChestGenHooks.DUNGEON_CHEST).addItem(loot);
		if (bonus) {
			ChestGenHooks.getInfo(ChestGenHooks.BONUS_CHEST).addItem(loot);
		}
	}
}
