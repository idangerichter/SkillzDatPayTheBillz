package bots;

import pirates.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MyBot implements PirateBot{
    PirateGame game;
    boolean DEBUG = true;
    Random randomGen = new Random();
    List<Pirate> defenceSquad;

    final int OUR_CITY_RANGE = 10;
    final int ENEMY_CITY_RANGE = 6;

    public void debug(String s){
        if(DEBUG)
        {
            game.debug(s);
        }
    }
    public void debug(int s){
        if(DEBUG)
        {
            game.debug(s);
        }
    }

    @Override
    public void doTurn(PirateGame _game) {
        game = _game;

        // Take care of all pirates ai
        handlePirates();
        // Take care of all drones ai
        handleDrones();
    }

    public void handlePirates()
    {
        // Init stuff
        defenceSquad = null;
        List<Pirate> myAvailablePirates = game.getMyLivingPirates();
        List<Pirate> EnemyPirates = game.getEnemyLivingPirates();
        List<Island> islandsToConquer = game.getNotMyIslands();
        City myCity = game.getMyCities().get(0); // I know I don't need to init this every turn but I don't care
        City enemyCity = game.getEnemyCities().get(0);

        // Checks for spawn kill
        if(!EnemyPirates.isEmpty()) {
            int index = getClosestPirate(EnemyPirates, myCity.location);
            Pirate closestEnemyPirateToCity = EnemyPirates.get(index);
            if (closestEnemyPirateToCity.distance(myCity) <= OUR_CITY_RANGE) {
                defenceSquad = getSuicideSquad(myAvailablePirates,closestEnemyPirateToCity);
                for(Pirate p : defenceSquad)
                {
                    myAvailablePirates.remove(p);
                    if (canPirateShoot(p)) {
                        Aircraft aircraft = getClosestTarget(p);
                        game.attack(p, aircraft);
                    } else {
                        List<Location> sailOptions = game.getSailOptions(p, closestEnemyPirateToCity);
                        game.setSail(p, sailOptions.get(0));
                    }
                }
            }
        }


        //-------------------Spawn Kills ---------------------
        // Let's be assholes
        // Spawn kills for the win
        int chuckNorrisIndex = getClosestPirate(myAvailablePirates,enemyCity.location);
        Pirate chuckNorris = myAvailablePirates.get(chuckNorrisIndex);
        myAvailablePirates.remove(chuckNorrisIndex);

        Aircraft target = getClosestTarget(chuckNorris);
        if (target!=null && target.distance(enemyCity.location)<=ENEMY_CITY_RANGE ){
            // Shoot drones and pirates near the enemy city
            if(canPirateShoot(chuckNorris))
            {
                game.attack(chuckNorris,target);
            }
            else{
                List<Location> sailOptions = game.getSailOptions(chuckNorris, target);
                game.setSail(chuckNorris, sailOptions.get(0));
            }
        }
        else{
            List<Location> sailOptions = game.getSailOptions(chuckNorris, enemyCity);
            game.setSail(chuckNorris,sailOptions.get(0));
        }

        //-------------------Send to islands------------------
        for (int i=0;i<myAvailablePirates.size();i++){
            if (islandsToConquer.isEmpty())
            {
                break;
            }
            Pirate p = myAvailablePirates.get(i);
            myAvailablePirates.remove(i);
            if(canPirateShoot(p))
            {
                Aircraft pirateTarget = getClosestTarget(p);
                game.attack(p,pirateTarget);

            }
            else {
                int islandIndex = getClosestIsland(islandsToConquer, p.location);
                Island island = islandsToConquer.get(islandIndex);
                List<Location> sailOptions = game.getSailOptions(p, island);
                game.setSail(p, sailOptions.get(0));
                islandsToConquer.remove(islandIndex);
            }

        }

        //-----------------KILL THEM ALL ---------------------
        List<Aircraft> TargetBank = game.getEnemyLivingAircrafts();

        for(Pirate p: myAvailablePirates)
        {
            Aircraft aircraft = getClosestTarget(TargetBank,p);
            TargetBank.remove(aircraft);
            if(canPirateShoot(p))
            {
                game.attack(p,aircraft);
            }
            else{
                List<Location> sailOptions = game.getSailOptions(p, aircraft);
                game.setSail(p, sailOptions.get(0));
            }
        }
    }

    public void handleDrones(){
        for (Drone drone : game.getMyLivingDrones()) {
            // Get my city
            City destination = game.getMyCities().get(0);
            // Get the sail options to the city
            List<Location> sailOptions = game.getSailOptions(drone, destination);

            // Generate random path to city
            int index = randomGen.nextInt(sailOptions.size());

            // Sail to the city
            if (defenceSquad==null){
                game.setSail(drone, sailOptions.get(index));
            }
            else {
                Pirate lastOfSquad = defenceSquad.get(defenceSquad.size()-1);
                if (drone.distance(destination)-4 <= drone.distance(lastOfSquad)){
                    List<Location> toEscort = game.getSailOptions(drone, lastOfSquad);
                    int index2 = randomGen.nextInt(toEscort.size());
                    game.setSail(drone, toEscort.get(index2));
                }
                else {
                    game.setSail(drone, sailOptions.get(index));
                }
            }
        }
    }

    // Returns target for pirate
    public boolean canPirateShoot(Pirate p) {
        for(Aircraft aircraft: game.getEnemyLivingAircrafts()){
            if (p.inAttackRange(aircraft))
                return true;
        }
        return false;
    }
    public Aircraft getClosestTarget(Pirate p){
        List<Aircraft> enemyAircrafts = game.getEnemyLivingAircrafts();
        if (enemyAircrafts.isEmpty()){
            return null;
        }
        Aircraft target = enemyAircrafts.get(0);
        int distance = p.distance(target);
        for (Aircraft a: enemyAircrafts) {
            if (p.distance(a) < distance){
                distance = p.distance(a);
                target = a;
            }
        }
        return target;
    }

    public Aircraft getClosestTarget(List<Aircraft> enemyAircrafts,Pirate p) {
        if (enemyAircrafts.isEmpty()){
            return null;
        }
        Aircraft target = enemyAircrafts.get(0);
        int distance = p.distance(target);
        for (Aircraft a: enemyAircrafts) {
            if (p.distance(a) < distance){
                distance = p.distance(a);
                target = a;
            }
        }
        return target;
    }

    public int getClosestPirate(List<Pirate> pirates, Location l) {
        int minDistance = 2100;
        int closestPirateIndex = 0;
        int id = 0;
        for(Pirate p: pirates) {
            int d = p.distance(l);
            if (d < minDistance){
                minDistance = d;
                closestPirateIndex = id;
            }
            id++;
        }
        return closestPirateIndex;
    }
    public int getClosestIsland(List<Island> islands, Location l) {
        int minDistance = 2100;
        int closestIslandIndex = 0;
        int id = 0;
        for(Island i: islands) {
            int d = i.distance(l);
            if (d < minDistance){
                minDistance = d;
                closestIslandIndex = id;
            }
            id++;
        }
        return closestIslandIndex;
    }

    public List<Pirate> getSuicideSquad(List<Pirate> _myPirates, Pirate target) {
        int squadHP = 0;
        List<Pirate> myPirates = new ArrayList(_myPirates);
        List<Pirate> squad = new ArrayList();
        while (squadHP < target.currentHealth) {
            Pirate nextPirate = myPirates.get(getClosestPirate(myPirates, target.getLocation()));
            squad.add(nextPirate);
            squadHP += nextPirate.currentHealth;
            myPirates.remove(nextPirate);
        }
        return squad;
    }
}
