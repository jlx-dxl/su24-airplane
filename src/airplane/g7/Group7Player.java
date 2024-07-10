package airplane.g7;

import java.util.ArrayList;
import java.util.Random;
import org.apache.log4j.Logger;
import airplane.sim.Plane;
import airplane.sim.Player;

public class Group7Player extends Player {

    private Logger logger = Logger.getLogger(this.getClass()); // 用于日志记录
    private Random random = new Random(); // 用于生成随机数

    private final double alpha = 1.0; // time 权重
    private final double beta = 1.0; // power 权重
    private final double gamma = 1.0; // delay 权重

    // 初始化模拟退火算法的温度和降温速率
    private double temperature = 1000;
    private double coolingRate = 0.999;

    private int timeCost = 0; // 总步数即为时间成本
    private int powerCost = 0;
    private int delayCost = 0;

    @Override
    public String getName() {
        return "Simulated Annealing Player";
    }

    @Override
    public void startNewGame(ArrayList<Plane> planes) {
        logger.info("Starting new game!");
        temperature = 1000;
        coolingRate = 0.999;

        timeCost = 0; // 总步数即为时间成本
        powerCost = 0;
        delayCost = 0;
    }

    @Override
    public double[] updatePlanes(ArrayList<Plane> planes, int round, double[] bearings) {
        // 初始航向
        double[] bestBearings = bearings.clone();
        // 初始起飞时间
        ArrayList<Integer> bestDepartureTimes = getInitialDepartureTimes(planes);
        // 初始损失
        double bestCost = calculateCost(planes, bestBearings, bestDepartureTimes, round);

        while (temperature > 1) {
            // 更新航向
            double[] newBearings = generateNewBearings(planes, bestBearings, round);

            ArrayList<Integer> newDepartureTimes;
            // 随机延后未起飞飞机的起飞时间
            if (bestDepartureTimes.stream().anyMatch(element -> round<=element)) {
                newDepartureTimes = generateNewDepartureTimes(planes, bestDepartureTimes);
            } else {
                newDepartureTimes = bestDepartureTimes;
            }

            // 计算cost
            double newCost = calculateCost(planes, newBearings, newDepartureTimes, round);

            // 根据温度决定是否接收解
            if (acceptanceProbability(bestCost, newCost, temperature) > random.nextDouble()) {
                bestBearings = newBearings.clone();
                bestDepartureTimes = new ArrayList<>(newDepartureTimes);
                bestCost = newCost;
            }

            // 温度下降
            temperature *= coolingRate;
        }

        return bestBearings;
    }

    private ArrayList<Integer> getInitialDepartureTimes(ArrayList<Plane> planes) {
        ArrayList<Integer> departureTimes = new ArrayList<>();
        for (Plane plane : planes) {
            departureTimes.add(plane.getDepartureTime());
        }
        return departureTimes;
    }

    private double[] generateNewBearings(ArrayList<Plane> planes, double[] bearings, int round) {
        // 当前航线
        double[] newBearings = bearings.clone();
        for (int i = 0; i < planes.size(); i++) {
            Plane p = planes.get(i);
            if (p.getBearing() == -1 && p.getDepartureTime() <= round) {   // 已经到达能起飞的时间且还未起飞
                // 正对目的地起飞
                newBearings[i] = calculateBearing(p.getLocation(), p.getDestination());
            } else if (p.getBearing() != -1 && p.getBearing() != -2) {   // 在空中（已起飞，还未到目的地）
                // 正对目的地飞行
                newBearings[i] = calculateBearing(p.getLocation(), p.getDestination());
            }
        }
        return newBearings;
    }

    // 高温阶段:
    // 在算法的初期，温度较高。此时，即使新的解比当前解差，算法也有较高的概率接受这个较差的解。
    // 这使得算法能够进行较大范围的搜索，探索更多的解空间，从而避免陷入局部最优。
    // 低温阶段:
    // 随着算法的进行，温度逐渐降低。此时，只有当新的解显著优于当前解时，算法才会接受这个新的解。
    // 这使得算法在搜索的后期更倾向于细致地优化当前解，逐步收敛到全局最优解。
    private ArrayList<Integer> generateNewDepartureTimes(ArrayList<Plane> planes, ArrayList<Integer> currentDepartureTimes) {
        ArrayList<Integer> newDepartureTimes = new ArrayList<>(currentDepartureTimes);
        for (int i = 0; i < planes.size(); i++) {
            int newTime = newDepartureTimes.get(i) + (random.nextInt(5)+1); // 在 1~5 中随机调整
            newDepartureTimes.set(i, Math.max(newTime, planes.get(i).getDepartureTime()));
        }
        return newDepartureTimes;
    }

    private double calculateCost(ArrayList<Plane> planes, double[] bearings, ArrayList<Integer> departureTimes, int round) {
        timeCost = round; // 总步数即为时间成本
        int totalPenalty = 0;

        for (int i = 0; i < planes.size(); i++) {
            Plane p = planes.get(i);
            if (p.getBearing() != -2) { // 只有在飞机还未到达目的地时才计算其代价
                if (p.getBearing() != -1) {
                    powerCost++; // 每飞行一步增加一个单位的飞行成本
                } else if (round > departureTimes.get(i)) {
                    if (timeCost>1) {
                        delayCost++; // 加入延迟起飞的代价
                    }
                }
            }
        }

        // 使用 logger 结构化输出每种成本和惩罚值
        logger.info(String.format("Time Cost: %3d, Power Cost: %3d, Delay Cost: %3d;",
                timeCost, powerCost, delayCost));

        return alpha * timeCost + beta * powerCost + gamma * delayCost;
    }

    private double acceptanceProbability(double currentCost, double newCost, double temperature) {
        if (newCost < currentCost) { // 如果新代价更低，直接接受
            return 1.0;
        }
        return Math.exp((currentCost - newCost) / temperature); // 否则根据温度计算接受概率
    }
}
