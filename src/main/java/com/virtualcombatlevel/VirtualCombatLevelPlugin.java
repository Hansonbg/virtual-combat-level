package com.virtualcombatlevel;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Experience;
import static net.runelite.api.Experience.getLevelForXp;
import net.runelite.api.IndexedObjectSet;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.WorldView;
import net.runelite.client.hiscore.HiscoreClient;
import net.runelite.client.hiscore.HiscoreEndpoint;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import static net.runelite.client.hiscore.HiscoreSkill.*;
import net.runelite.client.hiscore.HiscoreSkillType;
import net.runelite.api.events.ClientTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.hiscore.HiscoreEndpoint;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Virtual Combat Level",
	description = "Shows the virtual combat level of otherwise max-level players.",
	tags = {"combat", "level"}
)
public class VirtualCombatLevelPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private VirtualCombatLevelConfig config;

	@Inject
	private ClientThread clientThread;

	@Inject
	private HiscoreClient hiscoreClient;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Virtual Combat Plugin started!");
	}

	@Provides
	VirtualCombatLevelConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(VirtualCombatLevelConfig.class);
	}

	@Override
	protected void shutDown()
	{
		log.info("Virtual Combat Plugin stopped!");
	}

	@Subscribe
	public void onClientTick(ClientTick clientTick)
	{
		IndexedObjectSet<? extends Player> players = client.getTopLevelWorldView().players();

		for (Player player : players)
		{
			if(!(player.getCombatLevel() < 126))
			{
				calculateVirtualCombat(player);
			}
		}

	}

	private void calculateVirtualCombat(Player player)
	{
		try
		{
			HiscoreResult result = hiscoreClient.lookup(player.getName());

			int attackLvl = getLevelForXp((int) result.getSkill(ATTACK).getExperience());
			int strengthLvl = getLevelForXp((int) result.getSkill(STRENGTH).getExperience());
			int defenceLvl = getLevelForXp((int) result.getSkill(DEFENCE).getExperience());
			int hitpointsLvl = getLevelForXp((int) result.getSkill(HITPOINTS).getExperience());
			int magicLvl = getLevelForXp((int) result.getSkill(MAGIC).getExperience());
			int rangedLvl = getLevelForXp((int) result.getSkill(RANGED).getExperience());
			int prayerLvl = getLevelForXp((int) result.getSkill(PRAYER).getExperience());

			double base = 0.25 * (defenceLvl + hitpointsLvl + Math.floor(prayerLvl * 0.5));
			double melee = 0.325 * (attackLvl + strengthLvl);
			double range = 0.325 * Math.floor(rangedLvl * 1.5);
			double magic = 0.325 * Math.floor(magicLvl * 1.5);

			int virtualCombatLvl = (int) Math.floor(base + Math.max(melee, Math.max(range, magic)));
		}
		catch (Exception e)
		{
			log.warn("Error fetching Hiscore data " + e.getMessage());
		}
	}
}
