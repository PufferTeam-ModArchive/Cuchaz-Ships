/*******************************************************************************
 * Copyright (c) 2013 jeff.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 * jeff - initial API and implementation
 ******************************************************************************/
package cuchaz.ships;

import static cuchaz.ships.Collider.PostMoveAction.*;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

public class Collider {

    private static class EntityMoveInfo {

        private boolean isInsideEntityMove;

        // save the pre-movement entity position
        private double oldX;
        private double oldY;
        private double oldZ;
        private double oldYSize;

        private static PostMoveAction postMoveAction;
    }

    public enum PostMoveAction {
        DO_NOTHING,
        SET_ON_GROUND_AND_COLLIDE,
        COLLIDE
    }

    private static ThreadLocal<EntityMoveInfo> entityMoveInfo = ThreadLocal.withInitial(EntityMoveInfo::new);

    public static void preEntityMove(Entity entity, double dx, double dy, double dz) {
        EntityMoveInfo info = entityMoveInfo.get();

        if (info.isInsideEntityMove) {
            throw new RuntimeException("Reentrance to Entity#moveEntity detected");
        }

        info.isInsideEntityMove = true;

        // no clip? Then it's easy
        if (entity.noClip) {
            info.postMoveAction = DO_NOTHING;
            return;
        }

        // save the pre-movement entity position
        info.oldX = entity.posX;
        info.oldY = entity.posY;
        info.oldZ = entity.posZ;
        info.oldYSize = entity.ySize;

        // NOTE: crouching players on ships will not be moved by entity.moveEntity()
        // because edge walk-over prevention doesn't know about ship blocks
        // it always sees the player as already over the edge, so any movement is prevented
        boolean isPlayerCrouching = entity.onGround && entity.isSneaking() && entity instanceof EntityPlayer;
        if (isPlayerCrouching && isEntityOnAnyShip(entity)) {
            // move the entity against the world without edge walk-over protections
            entity.onGround = false;
            info.postMoveAction = SET_ON_GROUND_AND_COLLIDE;
        } else {
            // collide with the world normally
            info.postMoveAction = COLLIDE;
        }
    }

    public static void postEntityMove(Entity entity, double dx, double dy, double dz) {
        EntityMoveInfo info = entityMoveInfo.get();

        if (!info.isInsideEntityMove) {
            throw new RuntimeException("Reexitance from Entity#moveEntity detected");
        }

        if (info.postMoveAction == SET_ON_GROUND_AND_COLLIDE) {
            entity.onGround = true;
        }

        if (info.postMoveAction != DO_NOTHING) {
            for (EntityShip ship : ShipLocator.getFromEntityLocation(entity)) {
                // collide with the ships
                ship.getCollider()
                    .onNearbyEntityMoved(info.oldX, info.oldY, info.oldZ, info.oldYSize, entity);
            }
        }

        info.isInsideEntityMove = false;
    }

    public static boolean isEntityOnShipLadder(EntityLivingBase entity) {
        for (EntityShip ship : ShipLocator.getFromEntityLocation(entity)) {
            if (ship.getCollider()
                .isEntityOnLadder(entity)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEntityOnAnyShip(Entity entity) {
        for (EntityShip ship : ShipLocator.getFromEntityLocation(entity)) {
            if (ship.getCollider()
                .isEntityAboard(entity)) {
                return true;
            }
        }
        return false;
    }
}
