package com.magicalmod.magic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Действия меню круга (ранее были командами). */
public final class RitualActions {
    private RitualActions() {}

    public static String handle(ServerPlayer player, String action) {
        return switch (action) {
            case "open" -> status(player);
            case "start" -> start(player);
            case "join" -> join(player);
            case "leave" -> leave(player);
            case "stop" -> stop(player);
            default -> "§cНеизвестное действие";
        };
    }

    public static String status(ServerPlayer player) {
        RitualCircle own = RitualManager.get(player.getUUID());
        if (own != null) {
            return "§6Ваш круг§f\nМана: " + Math.round(own.pool())
                    + "\nУчастников: " + own.contributorCount()
                    + "\nРадиус: " + Math.round(RitualCircle.RADIUS) + " блоков";
        }
        RitualCircle near = RitualManager.nearest(player);
        if (near != null) {
            ServerPlayer leader = player.level().getServer().getPlayerList().getPlayer(near.leader());
            return "§6Круг рядом§f\nЛидер: " + (leader == null ? "?" : leader.getName().getString())
                    + "\nМана: " + Math.round(near.pool())
                    + "\nУчастников: " + near.contributorCount();
        }
        return "§7Активного круга рядом нет.\nСоздайте круг или вступите в существующий.";
    }

    private static String start(ServerPlayer player) {
        if (RitualManager.get(player.getUUID()) != null) return "§cУ вас уже есть круг";
        RitualManager.start(player);
        RitualManager.notifyEveryone(player, Component.literal(
                "§a" + player.getName().getString() + " §fсоздал круг групповой магии!"
        ));
        player.displayClientMessage(Component.literal(
                "§aКруг создан. Игроки в радиусе " + Math.round(RitualCircle.RADIUS)
                        + " блоков могут открыть меню (G) и нажать «Вступить»"
        ), false);
        return status(player);
    }

    private static String join(ServerPlayer player) {
        RitualCircle circle = RitualManager.nearest(player);
        if (circle == null) return "§cРядом нет активного круга";
        if (circle.leader().equals(player.getUUID())) return "§cВы лидер этого круга";
        if (circle.isContributor(player.getUUID())) return "§cВы уже в круге";
        float mana = ManaManager.get(player);
        if (mana <= 0.0F) return "§cУ вас нет маны";

        circle.addContribution(player.getUUID(), mana);
        ManaManager.set(player, 0.0F);
        RitualManager.notifyEveryone(player, Component.literal(
                "§a" + player.getName().getString() + " §fотдал " + Math.round(mana)
                        + " маны в круг. Всего: §6" + Math.round(circle.pool())
        ));
        return status(player);
    }

    private static String leave(ServerPlayer player) {
        boolean left = RitualManager.leave(player);
        return left ? "§eВы вышли из круга (мана не возвращается)" : "§cВы не участвуете в круге";
    }

    private static String stop(ServerPlayer player) {
        RitualCircle circle = RitualManager.get(player.getUUID());
        if (circle == null) return "§cУ вас нет активного круга";
        RitualManager.stop(player);
        player.displayClientMessage(Component.literal("§eКруг расформирован, мана возвращена"), false);
        return status(player);
    }
}
