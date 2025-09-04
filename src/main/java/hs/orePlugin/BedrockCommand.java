package hs.orePlugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BedrockCommand implements CommandExecutor {

    private final OreAbilitiesPlugin plugin;

    public BedrockCommand(OreAbilitiesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        AbilityActivationManager activationManager = plugin.getActivationManager();

        switch (label.toLowerCase()) {
            case "bedrock":
                return handleBedrockToggle(player, activationManager);
            case "ability":
                return handleAbilityCommand(player);
            default:
                return false;
        }
    }

    private boolean handleBedrockToggle(Player player, AbilityActivationManager activationManager) {
        boolean currentMode = activationManager.isBedrockMode(player);
        activationManager.setBedrockMode(player, !currentMode);
        return true;
    }

    private boolean handleAbilityCommand(Player player) {
        AbilityActivationManager activationManager = plugin.getActivationManager();

        if (!activationManager.isBedrockMode(player)) {
            player.sendMessage("§cThis command is only available in Bedrock mode!");
            player.sendMessage("§eUse §6/bedrock §eto enable Bedrock mode.");
            return true;
        }

        // Activate ability
        boolean success = plugin.getAbilityManager().useAbility(player);

        if (success) {
            player.sendMessage("§aAbility activated!");
        }

        return true;
    }
}