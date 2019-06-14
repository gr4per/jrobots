public class __GamacX_ extends JJRobot {

	static final int MISSILE_RANGE = 700; // 700 meters
	static final int MISSILE_SPEED = 300; // 300 m/s

	static final int MAX_X = 1000; // 1000 meters
	static final int MAX_Y = 1000; // 1000 meters

	static final int SAFE_DISTANCE = 20; // 15 meters

	private double last_shot_time;

	private int fire(int angle, int range) {
		int was_fired = cannon(angle, range);

		if (was_fired == 1) {
			last_shot_time = time();
		}

		return was_fired;
	}

	private boolean isReadyToFire() {
		if ((time() - last_shot_time) >= 1)
			return true;
		else
			return false;
	}

	private void sleep(int seconds) {
		double current_time = time();
		while ((time() - current_time) < seconds)
			;
	}

	private int move(int angle, int speed) {
		int x = loc_x();
		int y = loc_y();

		int sleep = rand(2);

		if (x >= (MAX_X - SAFE_DISTANCE)) {
			sleep *= 2;

			if (y <= MAX_Y / 2) {
				angle = 90 + rand(30);
			} else {
				angle = 270 - rand(30);
			}

		} else if (x <= SAFE_DISTANCE) {
			sleep *= 2;

			if (y <= MAX_Y / 2) {
				angle = 90 - rand(30);
			} else {
				angle = 270 + rand(30);
			}

		} else if (y >= (MAX_Y - SAFE_DISTANCE)) {
			sleep *= 2;

			if (x >= MAX_X / 2) {
				angle = 180 + rand(30);
			} else {
				angle = 360 - rand(30);
			}

		} else if (y <= (SAFE_DISTANCE)) {
			sleep *= 2;

			if (x >= MAX_X / 2) {
				angle = 180 - rand(30);
			} else {
				angle = 0 + rand(30);
			}
		}

		drive(angle, speed);

		sleep(sleep);

		drive(angle, 0);

		return angle;
	}

	private void evade(int degree) {
		int angle = (degree - 180 - rand(90)) % 360;

		angle = move(angle, 100);
	}

	void main() {

		int scan_degree = rand(360);
		int scan_amplitude = 1;
		int scan_range = 0;
		int damage = damage();

		while (true) {

			while ((scan_range = scan(scan_degree, scan_amplitude)) > 0) {

				if (scan_range <= 700) {
					fire(scan_degree, scan_range); // angle, range
				}

				if ((scan_range >= 50) /*|| (scan_range > 700)*/) {
					drive(scan_degree, 50);
					sleep(1);
					drive(scan_degree, 0);
				}
			}

			if (damage != damage()) {
				damage = damage();
				evade(scan_degree);
			}

			scan_degree += scan_amplitude;
			scan_degree %= 360;
		}
	}
}
