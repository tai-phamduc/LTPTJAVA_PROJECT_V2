import dao.*;

import entity.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 4631;
    private static final int MAX_THREADS = 100;

    private static final AccountDAO accountDAO = new AccountDAO();
    private static final CoachDAO coachDAO = new CoachDAO();
    private static final TrainDAO trainDAO = new TrainDAO();
    private static final LineDAO lineDAO = new LineDAO();
    private static final StationDAO stationDAO = new StationDAO();
    private static final StopDAO stopDAO = new StopDAO();
    private static final TicketDAO ticketDAO = new TicketDAO();
    private static final TicketDetailDAO ticketDetailDAO = new TicketDetailDAO();
    private static final TotalIncomeDAO totalIncomeDAO = new TotalIncomeDAO();
    private static final TrainJourneyDAO trainJourneyDAO = new TrainJourneyDAO();
    private static final TrainJourneyRankingDAO trainJourneyRankingDAO = new TrainJourneyRankingDAO();

    private static final ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS);

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down server gracefully...");
            threadPool.shutdown();
        }));

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("✅ Server started on port " + PORT);

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Socket socket = serverSocket.accept();
                    System.out.println("\n[CLIENT CONNECTED] " + socket.getRemoteSocketAddress());
                    threadPool.execute(() -> handleClient(socket));
                } catch (IOException e) {
                    if (!serverSocket.isClosed()) {
                        System.err.println("Error accepting client connection: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            threadPool.shutdown();
            System.out.println("Server shutdown complete");
        }
    }

    private static void handleClient(Socket socket) {
        try (socket;
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            Object request = in.readObject();

            // Log the incoming request
            System.out.println("\n[REQUEST FROM " + socket.getRemoteSocketAddress() + "]");
            System.out.println("Request Data: " + request);

            if (!(request instanceof HashMap<?, ?> map)) {
                String errorMsg = "Invalid request format";
                out.writeObject(errorMsg);
                System.out.println("[RESPONSE] " + errorMsg);
                return;
            }

            String type = (String) map.get("type");
            String action = (String) map.get("action");
            HashMap<String, String> payload = (HashMap<String, String>) map.get("payload");

            System.out.println("Type: " + type);
            System.out.println("Action: " + action);
            System.out.println("Payload: " + payload);

            Object result;
            try {
                result = switch (type) {
                    case "account" -> handleAccount(action, payload);
                    case "coach" -> handleCoach(action, payload);
                    case "train" -> handleTrain(action, payload);
                    case "line" -> handleLine(action, payload);
                    case "station" -> handleStation(action, payload);
                    case "stop" -> handleStop(action, payload);
                    case "ticket" -> handleTicket(action, payload);
                    case "ticketdetail" -> handleTicketDetail(action, payload);
                    case "income" -> handleIncome(action, payload);
                    case "trainjourney" -> handleTrainJourney(action, payload);
                    case "trainjourneyranking" -> handleTrainJourneyRanking(action, payload);
                    default -> "Unknown type: " + type;
                };
            } catch (Exception e) {
                result = "Error processing request: " + e.getMessage();
                System.err.println("[ERROR] Handling client request from " +
                        socket.getRemoteSocketAddress() + ": " + e.getMessage());
            }

            // Log the response before sending
            System.out.println("[RESPONSE TO " + socket.getRemoteSocketAddress() + "]");
            System.out.println("Response Data: " + result);

            out.writeObject(result);
            out.flush();
        } catch (IOException e) {
            System.err.println("[CLIENT ERROR] " + socket.getRemoteSocketAddress() + ": " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("[PROTOCOL ERROR] " + socket.getRemoteSocketAddress() + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[UNEXPECTED ERROR] " + socket.getRemoteSocketAddress() + ": " + e.getMessage());
        } finally {
            System.out.println("\n[CLIENT DISCONNECTED] " + socket.getRemoteSocketAddress());
        }
    }

    private static Object handleAccount(String action, HashMap<String, String> payload) {
        try {
            System.out.println("[ACCOUNT ACTION] " + action + " with payload: " + payload);
            Object result = switch (action) {
                case "getByUsername" -> accountDAO.getAccountByUsername(payload.get("username"));
                case "checkAvailability" -> accountDAO.checkAvalibility(payload.get("username"));
                case "updatePassword" ->
                        accountDAO.updatePassword(payload.get("employeeID"), payload.get("newPassword"));
                case "checkCredentials" ->
                        accountDAO.checkCredentials(payload.get("username"), payload.get("password"));
                case "getEmployeeByAccount" ->
                        accountDAO.getEmployeeByAccount(payload.get("username"), payload.get("password"));
                case "createAccount" -> {
                    Account account = new Account();
                    account.setUsername(payload.get("username"));
                    account.setPassword(payload.get("password"));
                    Employee employee = new Employee(payload.get("employeeID"));
                    account.setEmployee(employee);
                    yield accountDAO.createAccount(account);
                }
                case "getEmployeeByUsername" ->
                        accountDAO.getEmployeeByUsername(payload.get("username"),
                                Boolean.parseBoolean(payload.get("authentication")));
                case "getUserByEmployeeID" ->
                        accountDAO.getUserByEmployeeID(payload.get("employeeID"));
                case "getAccountByEmployeeID" ->
                        accountDAO.getAccountByEmployeeID(payload.get("employeeID"));
                case "updateAccount" ->
                        accountDAO.updateAccount(
                                payload.get("employeeID"),
                                payload.get("username"),
                                payload.get("password")
                        );
                default -> "Unknown account action: " + action;
            };
            System.out.println("[ACCOUNT RESULT] " + result);
            return result;
        } catch (Exception e) {
            System.err.println("[ACCOUNT ERROR] " + e.getMessage());
            return "Error processing account request: " + e.getMessage();
        }
    }

    private static Object handleCoach(String action, HashMap<String, String> payload) {
        try {
            System.out.println("[COACH ACTION] " + action + " with payload: " + payload);
            Object result = switch (action) {
                case "addCoach" -> {
                    int coachNumber = Integer.parseInt(payload.get("coachNumber"));
                    String coachType = payload.get("coachType");
                    int capacity = Integer.parseInt(payload.get("capacity"));
                    String trainID = payload.get("trainID");
                    Coach coach = new Coach(coachNumber, coachType, capacity, new Train(trainID, "", ""));
                    yield coachDAO.addCoach(coach);
                }
                case "getCoaches" -> {
                    Train train = new Train(payload.get("trainID"), "", "");
                    List<Coach> coaches = coachDAO.getCoaches(train);
                    yield coaches != null ? coaches : new ArrayList<Coach>();
                }
                case "removeCoaches" ->
                        coachDAO.removeCoaches(new Train(payload.get("trainID"), "", ""));
                case "getCoachByID" ->
                        coachDAO.getCoachByID(Integer.parseInt(payload.get("coachID")));
                default -> "Unknown coach action: " + action;
            };
            System.out.println("[COACH RESULT] " + result);
            return result;
        } catch (Exception e) {
            System.err.println("[COACH ERROR] " + e.getMessage());
            return "Error processing coach request: " + e.getMessage();
        }
    }

    private static Object handleTrain(String action, HashMap<String, String> payload) {
        try {
            System.out.println("[TRAIN ACTION] " + action + " with payload: " + payload);
            Object result = switch (action) {
                case "getAllTrainDetails" -> trainDAO.getAllTrainDetails();
                case "addNewTrain" -> {
                    Train train = new Train("", payload.get("trainNumber"), "");
                    train.setStatus(payload.get("status"));
                    yield trainDAO.addNewTrain(train);
                }
                case "deleteTrainByID" -> trainDAO.deleteTrainByID(payload.get("trainID"));
                case "getTrainByID" -> trainDAO.getTrainByID(payload.get("trainID"));
                case "getNumberOfCoaches" ->
                        trainDAO.getNumberOfCoaches(new Train(payload.get("trainID"), "", ""));
                case "updateTrain" -> {
                    Train train = new Train(
                            payload.get("trainID"),
                            payload.get("trainNumber"),
                            payload.get("status")
                    );
                    yield trainDAO.updateTrain(train);
                }
                case "getTrainDetailsByTrainNumber" ->
                        trainDAO.getTrainDetailsByTrainNumber(payload.get("trainNumber"));
                case "getAllTrain" -> trainDAO.getAllTrain();
                default -> "Unknown train action: " + action;
            };
            System.out.println("[TRAIN RESULT] " + result);
            return result;
        } catch (Exception e) {
            System.err.println("[TRAIN ERROR] " + e.getMessage());
            return "Error processing train request: " + e.getMessage();
        }
    }

    private static Object handleLine(String action, HashMap<String, String> payload) {
        try {
            System.out.println("[LINE ACTION] " + action + " with payload: " + payload);
            Object result = switch (action) {
                case "getAllLine" -> lineDAO.getAllLine();
                case "getAllLineDetailsByName" ->
                        lineDAO.getAllLineDetailsByName(payload.get("lineName"));
                case "getLineStops" ->
                        lineDAO.getLineStops(payload.get("lineID"));
                case "getAllLineDetails" ->
                        lineDAO.getAllLineDetails();
                case "removeLineByID" ->
                        lineDAO.removeLineByID(payload.get("lineID"));
                case "addLine" ->
                        lineDAO.addLine(payload.get("lineName"));
                case "addLineStop" -> {
                    StationLine stationLine = new StationLine(
                            Integer.parseInt(payload.get("index")),
                            new Station(payload.get("stationID")),
                            Integer.parseInt(payload.get("distance"))
                    );
                    yield lineDAO.addLineStop(payload.get("lineID"), stationLine);
                }
                case "getLineByID" ->
                        lineDAO.getLineByID(payload.get("lineID"));
                case "getLineStopByLineID" ->
                        lineDAO.getLineStopByLineID(payload.get("lineID"));
                case "updateLine" -> {
                    Line line = new Line(
                            payload.get("lineID"),
                            payload.get("lineName")
                    );
                    yield lineDAO.updateLine(line);
                }
                case "removeAllByLineID" ->
                        lineDAO.removeAllByLineID(payload.get("lineID"));
                default -> "Unknown line action: " + action;
            };
            System.out.println("[LINE RESULT] " + result);
            return result;
        } catch (Exception e) {
            System.err.println("[LINE ERROR] " + e.getMessage());
            return "Error processing line request: " + e.getMessage();
        }
    }

    private static Object handleStation(String action, HashMap<String, String> payload) {
        try {
            System.out.println("[STATION ACTION] " + action + " with payload: " + payload);
            Object result = switch (action) {
                case "getAllStation" -> stationDAO.getAllStation();
                case "addNewStation" ->
                        stationDAO.addNewStation(payload.get("stationName"));
                case "deleteStationByID" ->
                        stationDAO.deleteStationByID(payload.get("stationID"));
                case "findStationByName" ->
                        stationDAO.findStationByName(payload.get("stationName"));
                case "getStationByID" ->
                        stationDAO.getStationByID(payload.get("stationID"));
                case "updateStation" -> {
                    Station station = new Station(
                            payload.get("stationID"),
                            payload.get("stationName")
                    );
                    yield stationDAO.updateStation(station);
                }
                case "getStationsForTicket" ->
                        stationDAO.getStationsForTicket(payload.get("ticketID"));
                default -> "Unknown station action: " + action;
            };
            System.out.println("[STATION RESULT] " + result);
            return result;
        } catch (Exception e) {
            System.err.println("[STATION ERROR] " + e.getMessage());
            return "Error processing station request: " + e.getMessage();
        }
    }

    private static Object handleStop(String action, HashMap<String, String> payload) {
        try {
            System.out.println("[STOP ACTION] " + action + " with payload: " + payload);
            Object result = switch (action) {
                case "updateStop" -> {
                    Stop stop = new Stop(
                            payload.get("stopID"),
                            LocalDate.parse(payload.get("departureDate")),
                            LocalTime.parse(payload.get("arrivalTime")),
                            LocalTime.parse(payload.get("departureTime"))
                    );
                    yield stopDAO.updateStop(stop);
                }
                default -> "Unknown stop action: " + action;
            };
            System.out.println("[STOP RESULT] " + result);
            return result;
        } catch (Exception e) {
            System.err.println("[STOP ERROR] " + e.getMessage());
            return "Error processing stop request: " + e.getMessage();
        }
    }

    private static Object handleTicket(String action, HashMap<String, String> payload) {
        try {
            System.out.println("[TICKET ACTION] " + action + " with payload: " + payload);
            Object result = switch (action) {
                case "fetchEligibleRefundTicketsForOrder" -> {
                    boolean isRefund = Boolean.parseBoolean(payload.get("isRefund"));
                    yield ticketDAO.fetchEligibleRefundTicketsForOrder(payload.get("orderID"), isRefund);
                }
                case "getTicketByID" ->
                        ticketDAO.getTicketByID(payload.get("ticketID"));
                case "addTicket" -> {
                    TrainJourney trainJourney = new TrainJourney(payload.get("trainJourneyID"));
                    Seat seat = new Seat(Integer.parseInt(payload.get("seatID")));
                    Passenger passenger = new Passenger(payload.get("passengerID"));
                    Order order = new Order(payload.get("orderID"));
                    yield ticketDAO.addTicket(trainJourney, seat, passenger, order);
                }
                case "reassignTicketToNewOrder" ->
                        ticketDAO.reassignTicketToNewOrder(payload.get("newOrderID"), payload.get("ticketID"));
                case "updateTicketStatus" ->
                        ticketDAO.updateTicketStatus(payload.get("status"), payload.get("ticketID"));
                default -> "Unknown ticket action: " + action;
            };
            System.out.println("[TICKET RESULT] " + result);
            return result;
        } catch (Exception e) {
            System.err.println("[TICKET ERROR] " + e.getMessage());
            return "Error processing ticket request: " + e.getMessage();
        }
    }

    private static Object handleTicketDetail(String action, HashMap<String, String> payload) {
        try {
            System.out.println("[TICKETDETAIL ACTION] " + action + " with payload: " + payload);
            Object result = switch (action) {
                case "themChiTietVe" -> {
                    Stop stop = new Stop(payload.get("stopID"));
                    Ticket ticket = new Ticket(payload.get("ticketID"));
                    ticketDetailDAO.themChiTietVe(stop, ticket);
                    yield true; // Return success
                }
                default -> "Unknown ticketdetail action: " + action;
            };
            System.out.println("[TICKETDETAIL RESULT] " + result);
            return result;
        } catch (Exception e) {
            System.err.println("[TICKETDETAIL ERROR] " + e.getMessage());
            return "Error processing ticketdetail request: " + e.getMessage();
        }
    }

    private static Object handleIncome(String action, HashMap<String, String> payload) {
        try {
            System.out.println("[INCOME ACTION] " + action + " with payload: " + payload);
            Object result = switch (action) {
                case "getTotalIncome" -> {
                    int month = Integer.parseInt(payload.get("month"));
                    int year = Integer.parseInt(payload.get("year"));
                    yield totalIncomeDAO.getTotalIncome(month, year);
                }
                default -> "Unknown income action: " + action;
            };
            System.out.println("[INCOME RESULT] " + result);
            return result;
        } catch (Exception e) {
            System.err.println("[INCOME ERROR] " + e.getMessage());
            return "Error processing income request: " + e.getMessage();
        }
    }

    private static Object handleTrainJourney(String action, HashMap<String, String> payload) {
        try {
            System.out.println("[TRAINJOURNEY ACTION] " + action + " with payload: " + payload);
            Object result = switch (action) {
                case "getAllTrainJourneyDetails" -> trainJourneyDAO.getAllTrainJourneyDetails();
                case "addStops" -> {
                    List<Stop> stopList = new ArrayList<>();

                    // Parse the payload to create Stop objects
                    Stop stop = new Stop(
                            payload.get("stopID"), // You may need to generate this if not provided
                            new TrainJourney(payload.get("trainJourneyID")),
                            new Station(payload.get("stationID")),
                            Integer.parseInt(payload.get("stopOrder")),
                            Integer.parseInt(payload.get("distance")),
                            LocalDate.parse(payload.get("departureDate")),
                            LocalTime.parse(payload.get("arrivalTime")),
                            LocalTime.parse(payload.get("departureTime"))
                    );

                    stopList.add(stop);
                    yield trainJourneyDAO.addStops(stopList);
                }
                case "deleteTrainJourneyByID" ->
                        trainJourneyDAO.deleteTrainJourneyByID(payload.get("trainJourneyID"));
                case "getTrainJourneyByID" ->
                        trainJourneyDAO.getTrainJourneyByID(payload.get("trainJourneyID"));
                case "getAllStops" ->
                        trainJourneyDAO.getAllStops(new TrainJourney(payload.get("trainJourneyID")));
                case "updateTrainJourney" -> {
                    TrainJourney trainJourney = new TrainJourney(payload.get("trainJourneyID"));
                    trainJourney.setTraInJourneyName(payload.get("trainJourneyName"));
                    trainJourney.setBasePrice(Double.parseDouble(payload.get("basePrice")));
                    yield trainJourneyDAO.updateTrainJourney(trainJourney);
                }
                case "getAllTrainJourneyDetailsByTrainNumber" ->
                        trainJourneyDAO.getAllTrainJourneyDetailsByTrainNumber(payload.get("trainNumber"));
                case "addTrainJourney" -> {
                    TrainJourney trainJourney = new TrainJourney();
                    trainJourney.setTraInJourneyName(payload.get("trainJourneyName"));
                    trainJourney.setTrain(new Train(payload.get("trainID"), "", ""));
                    trainJourney.setLine(new Line(payload.get("lineID")));
                    trainJourney.setBasePrice(Double.parseDouble(payload.get("basePrice")));
                    yield trainJourneyDAO.addTrainJourney(trainJourney);
                }
                case "getDistanceBetweenTwoStops" ->
                        trainJourneyDAO.getDistanceBetweenTwoStopsOfATrainJourney(
                                payload.get("trainJourneyID"),
                                new Station(payload.get("departureStationID")),
                                new Station(payload.get("arrivalStationID"))
                        );
                case "searchTrainJourney" ->
                        trainJourneyDAO.searchTrainJourney(
                                payload.get("departureStation"),
                                payload.get("arrivalStation"),
                                LocalDate.parse(payload.get("departureDate"))
                        );
                case "getUnavailableSeats" ->
                        trainJourneyDAO.getUnavailableSeats(
                                payload.get("trainJourneyID"),
                                new Station(payload.get("departureStationID")),
                                new Station(payload.get("arrivalStationID"))
                        );
                case "getStops" ->
                        trainJourneyDAO.getStops(
                                new TrainJourney(payload.get("trainJourneyID")),
                                new Station(payload.get("departureStationID")),
                                new Station(payload.get("arrivalStationID"))
                        );
                default -> "Unknown train journey action: " + action;
            };
            System.out.println("[TRAINJOURNEY RESULT] " + result);
            return result;
        } catch (Exception e) {
            System.err.println("[TRAINJOURNEY ERROR] " + e.getMessage());
            return "Error processing train journey request: " + e.getMessage();
        }
    }

    private static Object handleTrainJourneyRanking(String action, HashMap<String, String> payload) {
        try {
            System.out.println("[TRAINJOURNEYRANKING ACTION] " + action + " with payload: " + payload);
            Object result = switch (action) {
                case "getTop10TrainJourneyRanking" ->
                        trainJourneyRankingDAO.getTop10TrainJourneyRanking(
                                Integer.parseInt(payload.get("month")),
                                Integer.parseInt(payload.get("year"))
                        );
                default -> "Unknown train journey ranking action: " + action;
            };
            System.out.println("[TRAINJOURNEYRANKING RESULT] " + result);
            return result;
        } catch (Exception e) {
            System.err.println("[TRAINJOURNEYRANKING ERROR] " + e.getMessage());
            return "Error processing train journey ranking request: " + e.getMessage();
        }
    }
}