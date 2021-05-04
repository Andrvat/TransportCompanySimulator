import lombok.Builder;
import utilities.LoggerPrintAssistant;

import javax.naming.TimeLimitExceededException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

@Builder
public class Train extends Thread {
    private static final Logger logger = Logger.getLogger(Train.class.getName());

    private final Properties trainProperties;

    private final TrainInformationLog informationLog;

    private int totalTimeInTrips = 0;

    private final ArrayList<Good> transportedGoods = new ArrayList<>();

    public Properties getTrainProperties() {
        return trainProperties;
    }

    @Override
    public void run() {
        int amortizationTime = Integer.parseInt(trainProperties.getProperty("amortizationTime"));
        while (totalTimeInTrips < amortizationTime) {
            RailwayTrack departureFreeRailwayTrackForSending;
            try {
                departureFreeRailwayTrackForSending = informationLog.getDepartureStation().getFreeStationRailwayTrack();
            } catch (InterruptedException exception) {
                LoggerPrintAssistant.printMessageWithSpecifiedThreadName(logger, Level.WARNING,
                        "Use of train was stopped while waiting for free departure track", exception);
                return;
            }

            try {
                informationLog.getDepartureStation().loadTrainWithGoodsByCapacities(this);
            } catch (InterruptedException exception) {
                LoggerPrintAssistant.printMessageWithSpecifiedThreadName(logger, Level.WARNING,
                        "Use of train was stopped while waiting for goods in storages", exception);
                return;
            }

            RailwayTrack forwardRailwayTrack;
            try {
                forwardRailwayTrack = informationLog.getRailwayTracksManager().getFreeForwardRailwayTrack();
                informationLog.getDepartureStation().freeOccupiedStationRailwayTrack(departureFreeRailwayTrackForSending);
            } catch (InterruptedException exception) {
                LoggerPrintAssistant.printMessageWithSpecifiedThreadName(logger, Level.WARNING,
                        "Use of train was stopped while waiting for free forward tracks", exception);
                return;
            }

            try {
                informationLog.getRailwayTracksManager().startTrainRunningOnTrack(this, forwardRailwayTrack);
            } catch (IOException | InterruptedException exception) {
                LoggerPrintAssistant.printMessageWithSpecifiedThreadName(logger, Level.WARNING,
                        "Use of train was stopped while waiting running on forward railway track", exception);
                return;
            } catch (TimeLimitExceededException exception) {
                LoggerPrintAssistant.printMessageWithSpecifiedThreadName(logger, Level.WARNING,
                        "Use of train was stopped due to exceeding the depreciation time during the forward trip", exception);
                informationLog.getDepartureStation().sendInfoToDepotForReplaceOldTrainToNewOne(this);
                return;
            }

            RailwayTrack arrivalFreeRailwayTrackForGetting;
            try {
                arrivalFreeRailwayTrackForGetting = informationLog.getArrivalStation().getFreeStationRailwayTrack();
                informationLog.getRailwayTracksManager().freeOccupiedForwardRailwayTrack(forwardRailwayTrack);
            } catch (InterruptedException exception) {
                LoggerPrintAssistant.printMessageWithSpecifiedThreadName(logger, Level.WARNING,
                        "Use of train was stopped while waiting for free arrival track", exception);
                return;
            }

            try {
                informationLog.getArrivalStation().unloadTrainWithGoodsToAssociateStorages(this);
            } catch (InterruptedException exception) {
                LoggerPrintAssistant.printMessageWithSpecifiedThreadName(logger, Level.WARNING,
                        "Use of train was stopped while waiting for free places in arrival storages", exception);
                return;
            }

            try {
                informationLog.getArrivalStation().freeOccupiedStationRailwayTrack(arrivalFreeRailwayTrackForGetting);
                Thread.sleep(1000);
            } catch (InterruptedException exception) {
                LoggerPrintAssistant.printMessageWithSpecifiedThreadName(logger, Level.WARNING,
                        "Use of train was stopped while busy-waiting in arrival station", exception);
                return;
            }
            
            RailwayTrack arrivalFreeRailwayTrackForSending;
            try {
                arrivalFreeRailwayTrackForSending = informationLog.getArrivalStation().getFreeStationRailwayTrack();
            } catch (InterruptedException exception) {
                LoggerPrintAssistant.printMessageWithSpecifiedThreadName(logger, Level.WARNING,
                        "Use of train was stopped while waiting for free arrival track", exception);
                return;
            }


            RailwayTrack backRailwayTrack;
            try {
                backRailwayTrack = informationLog.getRailwayTracksManager().getFreeBackRailwayTrack();
                informationLog.getArrivalStation().freeOccupiedStationRailwayTrack(arrivalFreeRailwayTrackForSending);
            } catch (InterruptedException exception) {
                LoggerPrintAssistant.printMessageWithSpecifiedThreadName(logger, Level.WARNING,
                        "Use of train was stopped while waiting for free back tracks", exception);
                return;
            }

            try {
                informationLog.getRailwayTracksManager().startTrainRunningOnTrack(this, backRailwayTrack);
            } catch (IOException | InterruptedException exception) {
                LoggerPrintAssistant.printMessageWithSpecifiedThreadName(logger, Level.WARNING,
                        "Use of train was stopped while waiting running on back railway track", exception);
                return;
            } catch (TimeLimitExceededException exception) {
                LoggerPrintAssistant.printMessageWithSpecifiedThreadName(logger, Level.WARNING,
                        "Use of train was stopped due to exceeding the depreciation time during the back trip", exception);
                informationLog.getDepartureStation().sendInfoToDepotForReplaceOldTrainToNewOne(this);
                return;
            }

            informationLog.getRailwayTracksManager().freeOccupiedBackRailwayTrack(backRailwayTrack);

            LoggerPrintAssistant.printMessageWithSpecifiedThreadName(logger, Level.WARNING,
                    "The train has completed a full cycle. The train proceeds to load the goods...");
        }
    }

    public void increaseTotalTimeInTripsByValue(int value) {
        totalTimeInTrips += value;
    }

    public int getTotalTimeInTrips() {
        return totalTimeInTrips;
    }

    public void loadGood(Good good) {
        transportedGoods.add(good);
    }

    public Good unloadNextGood() {
        return transportedGoods.remove(0);
    }

    public boolean areThereAnyUnloadedGoodsLeft() {
        return transportedGoods.size() > 0;
    }
}
