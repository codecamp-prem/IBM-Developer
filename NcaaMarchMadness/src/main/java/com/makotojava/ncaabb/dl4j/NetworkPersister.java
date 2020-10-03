package com.makotojava.ncaabb.dl4j;

import com.makotojava.ncaabb.util.NetworkUtils;
import org.apache.log4j.Logger;
import org.deeplearning4j.util.ModelSerializer;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class NetworkPersister {

  private static final Logger log = Logger.getLogger(NetworkPersister.class);

  private static final byte KEEP_LOOPING = -1;

  public static final String NETWORK_PARAMETERS_KEY = "network.parameters";

  public static void persistNetworks(final Scanner scanner,
                                     final List<NetworkCandidate> networkCandidateList) {
    Byte selectedNetworkNumber = displayNetworkSelectionMenu(scanner, networkCandidateList);
    saveSelectedNetworkOrNot(scanner,networkCandidateList, selectedNetworkNumber);
  }

  /**
   * Displays a menu of networks from the networkCandidates list, asks
   * the user to pick one, and returns their choice.
   */
  public static Byte displayNetworkSelectionMenu(final Scanner scanner,
                                            final List<NetworkCandidate> networkCandidates) {
    byte networkNumber = KEEP_LOOPING;
    while (networkNumber == KEEP_LOOPING && !networkCandidates.isEmpty()) {
      System.out.println("Enter the number of the network you want to persist (enter 0 to quit):");
      System.out.println("Network#         When Trained          Accuracy                    Layer Structure  Saved?");
      int index = 0;
      for (NetworkCandidate networkCandidate : networkCandidates) {
        NetworkParameters networkParameters = networkCandidate.getNetworkParameters();
        System.out.printf("   %d %24s        %f%%%35s  %b %n",
          index + 1,
          networkParameters.getWhenTrained().format(DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm")),
          networkParameters.getNetworkAccuracy() * 100.0,
          networkParameters.getNetworkLayout(),
          networkParameters.isNetworkSaved());
        index++;
      }
      if (scanner.hasNextByte()) {
        networkNumber = scanner.nextByte();
        if (networkNumber < 0 || networkNumber > networkCandidates.size()) {
          networkNumber = KEEP_LOOPING;
          continue;
        }
        if (networkNumber == 0) {
          break;
        }
      } else {
        System.out.printf("%s is not a valid choice.%n", scanner.next());
        networkNumber = KEEP_LOOPING;
      }
    }
    return networkNumber;
  }

  private static void saveSelectedNetworkOrNot(final Scanner scanner,
                                               final List<NetworkCandidate> networkCandidateList,
                                               final byte networkNumber) {
    String yesOrNo = null;
    while (yesOrNo == null) {
      System.out.printf("Save network %s (y/n)?%n", networkCandidateList.get(networkNumber - 1).getNetworkParameters().getNetworkLayout());
      yesOrNo = scanner.next().trim();
      if (yesOrNo.equalsIgnoreCase("y")) {
        NetworkCandidate networkCandidate = networkCandidateList.get(networkNumber - 1);
        if (saveNetwork(networkCandidate)) {
          networkCandidateList.remove(networkCandidate);
        }
      } else if (yesOrNo.equalsIgnoreCase("n")) {
        break;
      } else {
        yesOrNo = null; // Keep looping until we get something we understand
      }
    }
  }

  /**
   * Saves the specified network and returns true if successful, false otherwise.
   */
  private static boolean saveNetwork(final NetworkCandidate networkCandidate) {
    boolean ret = false;
    NetworkParameters networkParameters = networkCandidate.getNetworkParameters();
    String networkFileName = NetworkUtils.fetchNetworkDirectoryAndCreateIfNecessary() + File.separatorChar +
      String.format("NcaaBbNet-%s-%s.dl4j.zip", networkParameters.getNetworkLayout(), networkParameters.getWhenTrained().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")));
    System.out.printf("Saving network: %s as %s%n", networkParameters.getNetworkLayout(), networkFileName);
    try {
      File modelFile = new File(networkFileName);
      ModelSerializer.writeModel(networkCandidate.getMultiLayerNetwork(), modelFile, true);
      networkParameters.setNetworkSaved(true);
      ModelSerializer.addObjectToFile(modelFile, NETWORK_PARAMETERS_KEY, networkParameters);
      ret = true;
    } catch (IOException e) {
      String message = String.format("Error saving network file '%s': %s", networkFileName, e.getLocalizedMessage());
      log.error(message, e);
    }
    return ret;
  }
}
