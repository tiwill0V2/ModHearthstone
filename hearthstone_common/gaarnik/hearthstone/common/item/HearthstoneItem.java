package gaarnik.hearthstone.common.item;

import gaarnik.hearthstone.client.HearthstoneClientProxy;
import gaarnik.hearthstone.common.ModHearthstone;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityPortalFX;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;

public class HearthstoneItem extends Item {
	// *******************************************************************
	public static final int HEARTHSTONE_ID = 1500;

	private static final int MAX_DAMAGE = 10;
	private static final int MOTION_SICKNEWW_DURATION = 12000;

	// *******************************************************************

	// *******************************************************************
	public HearthstoneItem() {
		super(HEARTHSTONE_ID);

		this.setItemName("hearthstoneItem");
		this.setIconIndex(0);
		this.setMaxDamage(MAX_DAMAGE);
	}

	// *******************************************************************
	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int par7, float xRot, float yRot, float zRot) {
		if(world.isRemote)
			return false;

		int blockId = world.getBlockId(x, y, z);

		if(blockId == Block.bed.blockID)
			this.linkHearhtstone(stack, player, x, y, z);

		return true;
	}

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		if(world.isRemote == false) {
			if(player.getItemInUseCount() != 0)
				return stack;

			if(this.canUseHearthstone(player) == false)
				return stack;

			if(this.canTeleport(world, stack, player) == false)
				return stack;
		}

		player.setItemInUse(stack, this.getMaxItemUseDuration(stack));

		return stack;

	}

	public ItemStack onFoodEaten(ItemStack stack, World world, EntityPlayer player) {
		this.teleportPlayer(world, player, stack);

		return stack;
	}

	// *******************************************************************
	private void linkHearhtstone(ItemStack stack, EntityPlayer player, int bedX, int bedY, int bedZ) {
		if(stack.hasTagCompound() == false) {
			NBTTagCompound nbt = new NBTTagCompound();
			stack.setTagCompound(nbt);
		}

		NBTTagCompound nbt = stack.getTagCompound();
		nbt.setInteger("bedX", bedX);
		nbt.setInteger("bedY", bedY);
		nbt.setInteger("bedZ", bedZ);

		Vec3 position = player.getPosition(1.0f);

		nbt.setDouble("playerX", position.xCoord);
		nbt.setDouble("playerY", position.yCoord);
		nbt.setDouble("playerZ", position.zCoord);

		nbt.setFloat("rotationYaw", player.rotationYaw);
		nbt.setFloat("rotationYawHead", player.rotationYawHead);

		nbt.setBoolean("initialized", true);

		player.addChatMessage("Hearthstone linked !");
	}

	private boolean canUseHearthstone(EntityPlayer player) {
		PotionEffect sicknessEffect = player.getActivePotionEffect(ModHearthstone.heathstonePotion);

		if(sicknessEffect != null) {
			if(sicknessEffect.getDuration() != 0) {
				player.addChatMessage("You have motion sickness !");
				return false;
			}
			else
				player.removePotionEffect(ModHearthstone.heathstonePotion.id);
		}
		
		return true;
	}

	private boolean canTeleport(World world, ItemStack stack, EntityPlayer player) {
		if(stack.hasTagCompound() == false) {
			player.addChatMessage("You have not linked your Hearthstone !");
			return false;
		}

		NBTTagCompound nbt = stack.getTagCompound();

		if(nbt.getBoolean("initialized")) {
			nbt.setBoolean("initialized", false);
			return false;
		}

		int x = nbt.getInteger("bedX");
		int y = nbt.getInteger("bedY");
		int z = nbt.getInteger("bedZ");

		int blockId = world.getBlockId(x, y, z);

		if(blockId != Block.bed.blockID) {
			player.addChatMessage("Your bed is missing !");
			return false;
		}

		player.addChatMessage("Teleporting to home ...");
		
		return true;
	}
	
	private void teleportPlayer(World world, EntityPlayer player, ItemStack stack) {
		if(stack.hasTagCompound() == false) {
			player.addChatMessage("You have not linked your Hearthstone !");
			return;
		}
		
		NBTTagCompound nbt = stack.getTagCompound();
		
		int x = nbt.getInteger("bedX");
		int y = nbt.getInteger("bedY");
		int z = nbt.getInteger("bedZ");

		int blockId = world.getBlockId(x, y, z);

		if(blockId != Block.bed.blockID) {
			player.addChatMessage("Your bed is missing !");
			return;
		}

		/*int dimension = nbt.getInteger("dimension");
		EntityPlayerMP playerMP = (EntityPlayerMP) player;
		playerMP.travelToDimension(dimension);*/

		player.rotationYaw = nbt.getFloat("rotationYaw");
		player.rotationYawHead = nbt.getFloat("rotationYawHead");

		Vec3 position = player.getPosition(1.0f);
		this.spawnParticles(world, position.xCoord, position.yCoord, position.zCoord);

		double xCoord = nbt.getDouble("playerX");
		double yCoord = nbt.getDouble("playerY");
		double zCoord = nbt.getDouble("playerZ");

		player.setPositionAndUpdate(xCoord, yCoord, zCoord);

		if(ModHearthstone.DEBUG == false) {
			stack.damageItem(1, player);
			player.addPotionEffect(new PotionEffect(ModHearthstone.heathstonePotion.id, MOTION_SICKNEWW_DURATION, 0));
		}

		this.spawnParticles(world, xCoord, yCoord, zCoord);
	}
	
	private void spawnParticles(World world, double x, double y, double z) {
		EntityPortalFX effect = new EntityPortalFX(world, x, y, z, 1.0D, 0.0D, 0.0D);
		Minecraft.getMinecraft().effectRenderer.addEffect(effect, null);
	}

	// *******************************************************************
	public static void init() {
		HearthstoneItems.hearthstoneItem = new HearthstoneItem();

		GameRegistry.registerItem(HearthstoneItems.hearthstoneItem, "hearthstoneItem");
		LanguageRegistry.addName(HearthstoneItems.hearthstoneItem, "Hearthstone");

		ItemStack stack = new ItemStack(HearthstoneItems.hearthstoneItem, 1);

		if(ModHearthstone.DEBUG) {
			ItemStack dirstStack = new ItemStack(Block.dirt, 1);
			GameRegistry.addShapelessRecipe(stack, dirstStack);
		}
	}

	// *******************************************************************
	@Override
	public String getTextureFile() { return HearthstoneClientProxy.ITEMS_TEXTURE; }

	@Override
	public int getItemStackLimit() { return 1; }

	@Override
	public boolean isDamageable() { return true; }

	@Override
	public int getMaxItemUseDuration(ItemStack par1ItemStack) { return 32; }

	public EnumAction getItemUseAction(ItemStack par1ItemStack) { return EnumAction.bow; }

}
