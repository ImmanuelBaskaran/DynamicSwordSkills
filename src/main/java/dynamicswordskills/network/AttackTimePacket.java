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

package dynamicswordskills.network;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;

import cpw.mods.fml.relauncher.Side;

/**
 * 
 * Sets the player's attack time on either the client or the server
 *
 */
public class AttackTimePacket extends CustomPacket
{
	private int attackTime;

	public AttackTimePacket() {}
	
	public AttackTimePacket(int attackTime) {
		this.attackTime = attackTime;
	}

	@Override
	public void write(ByteArrayDataOutput out) throws IOException {
		out.writeInt(attackTime);
	}

	@Override
	public void read(ByteArrayDataInput in) throws IOException {
		attackTime = in.readInt();
	}

	@Override
	public void execute(EntityPlayer player, Side side) throws ProtocolException {
		player.attackTime = this.attackTime;
	}
}
