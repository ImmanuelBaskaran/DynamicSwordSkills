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

package dynamicswordskills.skills;

import java.util.List;

import dynamicswordskills.client.DSSClientEvents;
import dynamicswordskills.client.DSSKeyHandler;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.entity.EntityLeapingBlow;
import dynamicswordskills.network.PacketDispatcher;
import dynamicswordskills.network.bidirectional.ActivateSkillPacket;
import dynamicswordskills.ref.ModSounds;
import dynamicswordskills.util.PlayerUtils;
import dynamicswordskills.util.TargetUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 
 * LEAPING BLOW
 * Activation: Jump while holding block (shield is not required)
 * Damage: Regular sword damage (without enchantment bonuses), +1 extra damage per skill level
 * Effect: Adds Weakness I for (50 + (10 * level)) ticks
 * Range: Technique travels roughly 3 blocks + 1/2 block per level
 * Area: Approximately (0.5F + (0.25F * level)) radius in a straight line
 * Exhaustion: 2.0F minus 0.1F per level (1.5F at level 5)
 * 
 * Upon landing, all targets directly in front of the player take damage and
 * are weakened temporarily.
 * 
 */
public class LeapingBlow extends SkillActive
{
	/** Set to true when jumping and 'attack' key pressed; set to false upon landing */
	private boolean isActive = false;

	public LeapingBlow(String name) {
		super(name);
	}

	private LeapingBlow(LeapingBlow skill) {
		super(skill);
	}

	@Override
	public LeapingBlow newInstance() {
		return new LeapingBlow(this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(List<String> desc, EntityPlayer player) {
		desc.add(getDamageDisplay(level, true));
		desc.add(getRangeDisplay(3.0F + 0.5F * level));
		desc.add(getAreaDisplay(0.5F + 0.25F * level));
		desc.add(getDurationDisplay(getPotionDuration(player), false));
		desc.add(getExhaustionDisplay(getExhaustion()));
	}

	@Override
	public boolean isActive() {
		return isActive;
	}

	@Override
	protected float getExhaustion() {
		return 2.0F - (0.1F * level);
	}

	/**
	 * LeapingBlow adds exhaustion after entity is spawned, rather than on initial activation
	 */
	@Override
	protected boolean autoAddExhaustion() {
		return false;
	}

	/** Returns player's base damage (which includes all attribute bonuses) plus 1.0F per level */
	private float getDamage(EntityPlayer player) {
		return (float)(level + player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue());
	}

	/** Duration of weakness effect; used for tooltip display only */
	private int getPotionDuration(EntityPlayer player) {
		return (50 + (level * 10));
	}

	@Override
	public boolean canUse(EntityPlayer player) {
		return super.canUse(player) && !isActive() && PlayerUtils.isSwordOrProvider(player.getHeldItemMainhand(), this) && !TargetUtils.isInLiquid(player);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean canExecute(EntityPlayer player) {
		return !isActive() && player.onGround && isKeyPressed() && !TargetUtils.isInLiquid(player);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean isKeyListener(Minecraft mc, KeyBinding key) {
		return key == mc.gameSettings.keyBindJump;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean keyPressed(Minecraft mc, KeyBinding key, EntityPlayer player) {
		if (canExecute(player)) {
			PacketDispatcher.sendToServer(new ActivateSkillPacket(this));
			KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
			KeyBinding.setKeyBindState(DSSKeyHandler.keys[DSSKeyHandler.KEY_BLOCK].getKeyCode(), false);
			return true;
		}
		return false;
	}

	/**
	 * Returns true if either the custom block key or vanilla use item key is currently down
	 */
	@SideOnly(Side.CLIENT)
	protected boolean isKeyPressed() {
		return Minecraft.getMinecraft().gameSettings.keyBindUseItem.isKeyDown() || DSSKeyHandler.keys[DSSKeyHandler.KEY_BLOCK].isKeyDown();
	}

	@Override
	protected boolean onActivated(World world, EntityPlayer player) {
		isActive = true;
		return isActive();
	}

	@Override
	protected void onDeactivated(World world, EntityPlayer player) {
		isActive = false;
	}

	@Override
	public void onUpdate(EntityPlayer player) {
		// Handle on client because onGround is always true on the server
		if (player.getEntityWorld().isRemote && isActive() && (player.onGround || TargetUtils.isInLiquid(player))) {
			deactivate(player);
		}
	}

	/**
	 * Called from Forge fall Events (note that these are not fired if player lands in liquid!)
	 * @param distance distance fallen, passed from Forge fall Event
	 */
	public void onImpact(EntityPlayer player, float distance) {
		SwordBasic swordSkill = (SwordBasic) DSSPlayerInfo.get(player).getPlayerSkill(swordBasic);
		if (isActive() && swordSkill != null && swordSkill.isActive() && PlayerUtils.isSwordOrProvider(player.getHeldItemMainhand(), this)) {
			if (player.getEntityWorld().isRemote) {
				if (distance < 1.0F) {
					DSSClientEvents.performComboAttack(Minecraft.getMinecraft(), swordSkill);
				} else {
					player.swingArm(EnumHand.MAIN_HAND);
				}
			} else if (distance >= 1.0F) {
				// add exhaustion here, now that skill has truly activated:
				player.addExhaustion(getExhaustion());
				EntityLeapingBlow entity = new EntityLeapingBlow(player.getEntityWorld(), player).setDamage(getDamage(player)).setLevel(level);
				entity.shoot(player, player.rotationPitch, player.rotationYaw, 0.0F, entity.getVelocity(), 1.0F);
				player.getEntityWorld().spawnEntity(entity);
				PlayerUtils.playSoundAtEntity(player.getEntityWorld(), player, ModSounds.LEAPING_BLOW, SoundCategory.PLAYERS, 0.4F, 0.5F);
			}
		}
		onDeactivated(player.getEntityWorld(), player);
	}
}
