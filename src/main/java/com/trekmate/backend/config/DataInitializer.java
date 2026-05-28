package com.trekmate.backend.config;

import com.trekmate.backend.model.*;
import com.trekmate.backend.model.embeddable.DepartureGuideId;
import com.trekmate.backend.model.enums.*;
import com.trekmate.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;

import java.util.List;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * Khởi tạo dữ liệu seed khi server chạy lần đầu.
 * Kiểm tra bảng roles trống trước khi insert — an toàn khi restart nhiều lần.
 * Không dùng hardcoded UUID — id được sinh tự động bởi GenerationType.UUID.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository              userRepository;
    private final CustomerRepository          customerRepository;
    private final GuideRepository             guideRepository;
    private final TourRepository              tourRepository;
    private final TourWaypointRepository      tourWaypointRepository;
    private final TourDailyItineraryRepository itineraryRepository;
    private final ItineraryWaypointRepository  itineraryWaypointRepository;
    private final TourDepartureRepository     departureRepository;
    private final DepartureGuideRepository    departureGuideRepository;
    private final DepartureWeatherDailyRepository weatherDailyRepository;
    private final BookingRepository           bookingRepository;
    private final ReviewRepository            reviewRepository;
    private final EquipmentCategoryRepository equipmentCategoryRepository;
    private final PasswordEncoder             passwordEncoder;

    // ─── Wrapper giữ các entity đã seed để truyền giữa các bước ───────────────
    private record SeedUsers(
            User admin,
            User hoa, User khiem,
            Guide son, Guide mai, Guide anh) {}

    private record SeedTours(Tour fansipan, Tour taNang, Tour mapiLeng) {}

    // ───────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("[DataInitializer] Database đã có dữ liệu — bỏ qua seed.");
            return;
        }
        log.info("[DataInitializer] Database trống — bắt đầu seed dữ liệu...");

        seedEquipmentCategories();

        SeedUsers users = seedUsers();
        SeedTours tours = seedTours(users.admin());

        seedFansipanWaypointsAndItinerary(tours.fansipan());

        TourDeparture dep1  = seedDeparture(tours.fansipan(), "2025-06-02", "2025-06-04", "2025-05-30",
                "2800000", (short) 12, (short) 4, true,
                "Cổng trời Trạm Tôn, Sa Pa lúc 6:00",
                "Nắng đẹp, tầm nhìn xa", "sunny", (short) 14, (short) 22, DepartureStatus.OPEN,
                users.son());

        TourDeparture dep10 = seedDeparture(tours.fansipan(), "2025-04-10", "2025-04-12", "2025-04-08",
                "2600000", (short) 10, (short) 10, false,
                "Cổng trời Trạm Tôn, Sa Pa lúc 6:00",
                "Nắng đẹp suốt hành trình", "sunny", (short) 12, (short) 20, DepartureStatus.COMPLETED,
                users.son());
        updateCompletedDeparture(dep10);

        TourDeparture dep5 = seedDeparture(tours.taNang(), "2025-06-14", "2025-06-17", "2025-06-12",
                "1950000", (short) 15, (short) 7, false,
                "Sân UBND xã Tà Năng lúc 7:00",
                "Thời tiết lý tưởng trekking", "sunny", (short) 18, (short) 28, DepartureStatus.OPEN,
                users.mai());

        TourDeparture dep6 = seedDeparture(tours.mapiLeng(), "2025-06-21", "2025-06-22", "2025-06-19",
                "1500000", (short) 8, (short) 5, true,
                "Cột cờ Lũng Cú, Hà Giang lúc 6:30",
                "Mây mù sáng sớm, quang sau", "cloudy", (short) 17, (short) 25, DepartureStatus.OPEN,
                users.anh());

        seedWeatherForDeparture(dep1, tours.fansipan());

        seedBookingsAndReviews(users, tours.fansipan(), dep10, dep5, dep6);

        log.info("[DataInitializer] Seed hoàn tất.");
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Equipment Categories
    // ────────────────────────────────────────────────────────────────────────────

    private void seedEquipmentCategories() {
        String[][] data = {
            {"Lều trại",          "camping-tent",      "tent",     "1"},
            {"Túi ngủ",           "sleeping-bag",      "sleep",    "2"},
            {"Gậy & Dụng cụ leo", "trekking-poles",    "stick",    "3"},
            {"Balo",              "backpack",          "backpack", "4"},
            {"Quần áo kỹ thuật",  "technical-apparel", "jacket",   "5"},
            {"Thiết bị an toàn",  "safety-gear",       "helmet",   "6"},
            {"Đèn & Điện",        "lighting",          "torch",    "7"},
        };
        for (String[] c : data) {
            equipmentCategoryRepository.save(EquipmentCategory.builder()
                    .name(c[0]).slug(c[1]).icon(c[2])
                    .sortOrder(Short.parseShort(c[3])).build());
        }
        log.info("[Seed] {} equipment categories", data.length);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Users + Customers + Guides
    // ────────────────────────────────────────────────────────────────────────────

    private SeedUsers seedUsers() {
        String pw = passwordEncoder.encode("Test@1234");

        // ── Sơn: guide
        User userSon = createUser("son.nguyen@trailviet.vn", "0901234567", pw, false);
        createCustomer(userSon, "Nguyễn Văn Sơn", "1988-03-21", FitnessLevel.ATHLETE);
        Guide son = createGuide(userSon, "Nguyễn Văn Sơn",
                "HDV trekking 8 năm kinh nghiệm vùng Tây Bắc.", (short) 8,
                List.of(Map.of("name","Sơ cứu CPR","issued_by","Hội Chữ thập đỏ VN","year",2020)),
                List.of("vi","en"), List.of("high-altitude","winter-trekking","survival"),
                "Lào Cai", "4.98", 247, 312);

        // ── Mai: guide
        User userMai = createUser("mai.tran@trailviet.vn", "0902345678", pw, false);
        createCustomer(userMai, "Trần Thị Mai", "1990-07-08", FitnessLevel.ADVANCED);
        Guide mai = createGuide(userMai, "Trần Thị Mai",
                "Chuyên gia tuyến đường miền Trung và Tây Nguyên.", (short) 6,
                List.of(Map.of("name","Wilderness First Aid","issued_by","NOLS","year",2021)),
                List.of("vi","en","fr"), List.of("jungle-trekking","cultural-tour","family-friendly"),
                "Đà Nẵng", "4.95", 183, 241);

        // ── Anh: guide
        User userAnh = createUser("anh.pham@trailviet.vn", "0903456789", pw, false);
        Guide anh = createGuide(userAnh, "Phạm Hùng Anh",
                "Người con của đá Hà Giang, 10 năm chinh phục cung đường Đông Bắc.", (short) 10,
                List.of(Map.of("name","Rock Climbing Level 2","issued_by","Vietnam Mountaineering Federation","year",2018)),
                List.of("vi","en"), List.of("rock-climbing","extreme-trekking","karst-landscape"),
                "Hà Giang", "4.92", 418, 418);

        // ── Hoa & Khiêm: customer
        User hoa   = createUser("hoa@example.com",   "0911111111", pw, false);
        createCustomer(hoa, "Nguyễn Thị Hoa", "1992-05-14", FitnessLevel.INTERMEDIATE);

        User khiem = createUser("khiem@example.com", "0922222222", pw, false);
        createCustomer(khiem, "Phạm Gia Khiêm", "1995-09-30", FitnessLevel.BEGINNER);

        // ── Admin
        User admin = createUser("admin@trailviet.vn", "0800000001", pw, true);

        log.info("[Seed] Users, customers, guides created.");
        return new SeedUsers(admin, hoa, khiem, son, mai, anh);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Tours
    // ────────────────────────────────────────────────────────────────────────────

    private SeedTours seedTours(User admin) {
        Tour fansipan = tourRepository.save(Tour.builder()
                .title("Chinh phục Fansipan — Nóc nhà Đông Dương")
                .slug("fansipan-summit")
                .shortDescription("Hành trình chinh phục đỉnh Fansipan 3,147m hùng vĩ.")
                .difficulty(DifficultyLevel.HARD)
                .durationDays((short) 3).durationNights((short) 2)
                .distanceKm(new BigDecimal("19.0")).maxElevationM(3147)
                .startLocation("Sa Pa, Lào Cai").endLocation("Sa Pa, Lào Cai")
                .highlights(List.of("Đỉnh cao nhất Đông Dương 3147m","Rừng nguyên sinh Hoàng Liên","Sunrise trên mây"))
                .includes(List.of("HDV chuyên nghiệp","Lều trại","Bữa ăn trên đường","Cứu thương cơ bản"))
                .excludes(List.of("Vé cáp treo","Bảo hiểm du lịch","Chi phí cá nhân"))
                .requirements(List.of("Sức khoẻ tốt, leo bộ 8h/ngày","Kinh nghiệm trekking qua đêm"))
                .status(TourStatus.ACTIVE).createdBy(admin.getId()).build());

        Tour taNang = tourRepository.save(Tour.builder()
                .title("Trekking Tà Năng – Phan Dũng")
                .slug("ta-nang-phan-dung")
                .shortDescription("Cung đường đẹp nhất Việt Nam qua thảo nguyên xanh mướt.")
                .difficulty(DifficultyLevel.MODERATE)
                .durationDays((short) 4).durationNights((short) 3)
                .distanceKm(new BigDecimal("45.0")).maxElevationM(1920)
                .startLocation("Đà Lạt, Lâm Đồng").endLocation("Phan Thiết, Bình Thuận")
                .highlights(List.of("Thảo nguyên Tà Năng rộng lớn","Rừng thông cổ thụ","Suối Vàng"))
                .includes(List.of("HDV","Lều trại","Bữa ăn","Xe đưa đón điểm cuối"))
                .excludes(List.of("Bảo hiểm","Chi phí cá nhân"))
                .requirements(List.of("Sức khoẻ bình thường","Không yêu cầu kinh nghiệm trước"))
                .status(TourStatus.ACTIVE).createdBy(admin.getId()).build());

        Tour mapiLeng = tourRepository.save(Tour.builder()
                .title("Mã Pí Lèng — Đèo huyền thoại miền đá xám")
                .slug("ma-pi-leng-trek")
                .shortDescription("Chinh phục đèo Mã Pí Lèng và cao nguyên đá Đồng Văn.")
                .difficulty(DifficultyLevel.HARD)
                .durationDays((short) 2).durationNights((short) 1)
                .distanceKm(new BigDecimal("28.0")).maxElevationM(1300)
                .startLocation("Hà Giang").endLocation("Hà Giang")
                .highlights(List.of("Đèo Mã Pí Lèng hùng vĩ","Sông Nho Quế xanh biếc","Làng đá cổ Đồng Văn"))
                .includes(List.of("HDV địa phương","Lều trại","Bữa ăn"))
                .excludes(List.of("Di chuyển đến Hà Giang","Bảo hiểm"))
                .requirements(List.of("Sức khoẻ tốt","Không sợ độ cao"))
                .status(TourStatus.ACTIVE).createdBy(admin.getId()).build());

        log.info("[Seed] 3 tours created.");
        return new SeedTours(fansipan, taNang, mapiLeng);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Waypoints + Daily Itinerary (Fansipan)
    // ────────────────────────────────────────────────────────────────────────────

    private void seedFansipanWaypointsAndItinerary(Tour fansipan) {
        // ── Waypoints ──────────────────────────────────────────────────────────
        TourWaypoint wp1 = wp(fansipan, "Cổng Trạm Tôn", "tram-ton-gate", 1,
                WaypointType.TRAILHEAD, "22.3417", "103.7753", 1900, 1, false,
                "Điểm tập kết và xuất phát. Có bãi đỗ xe, nhà vệ sinh.",
                "Kiểm tra đầy đủ thiết bị trước khi lên. Phân phát bản đồ tuyến.",
                true, true, true, false,
                WaterSourceType.VILLAGE_TAP, "Vòi nước sạch ngay cổng. Đổ đầy bình trước khi đi.",
                null, null,
                "0203 871 975", "Quay về Trạm Tôn, xe về Sa Pa (30 phút)",
                "Bệnh viện Đa khoa Sa Pa", "8.0", false);

        TourWaypoint wp2 = wp(fansipan, "Nhà Gianh (2200m)", "nha-gianh-2200", 2,
                WaypointType.REST_STOP, "22.3285", "103.7698", 2200, 1, false,
                "Lán nhỏ bằng gỗ. Điểm nghỉ sau 2-3h đi bộ.",
                "Điểm điểm danh đoàn. Kiểm tra sức khoẻ sơ bộ.",
                false, true, false, true,
                WaterSourceType.STREAM, "Suối nhỏ 100m phía Đông Bắc. Đun sôi trước khi uống.",
                null, null,
                null, "Trở lại Cổng Trạm Tôn (1.5h)",
                "Bệnh viện Đa khoa Sa Pa", "12.0", false);

        TourWaypoint wp3 = wp(fansipan, "Bãi cắm trại 2800m", "camp-2800", 3,
                WaypointType.CAMP_SITE, "22.3089", "103.7635", 2800, 1, true,
                "Bãi đất phẳng ~200m², gần suối. Điểm nghỉ đêm 1.",
                "Cắm lều trước 17:00. Gió Tây mạnh sau 18:00, cố định cọc lều kỹ.",
                false, false, false, true,
                WaterSourceType.STREAM,
                "Suối 50m phía dưới. Nguồn nước CUỐI trước đỉnh — tích đủ 2L/người cho sáng mai.",
                AccommodationType.TENT_CAMPING, (short) 12,
                "0203 871 975",
                "Xuống Nhà Gianh (2h) → Trạm Tôn. Cần cáng nếu chấn thương.",
                "Bệnh viện Đa khoa Sa Pa", "18.0", false);

        TourWaypoint wp4 = wp(fansipan, "Điểm thoát hiểm 3000m", "emergency-3000", 4,
                WaypointType.EMERGENCY_POINT, "22.3021", "103.7602", 3000, 2, false,
                "Đường tắt xuống phía Đông khi thời tiết xấu đột ngột.",
                "NẾU sương mù < 20m sau điểm này → dừng lại, đánh giá. Không tiếp tục lên đỉnh.",
                false, false, false, true,
                WaterSourceType.NONE, null, null, null,
                "114", "Đường tắt Đông 3km, biển đỏ cắm tại đây. Xuống: 2.5h.",
                "Bệnh viện Đa khoa Sa Pa", "22.0", true);

        TourWaypoint wp5 = wp(fansipan, "Đỉnh Fansipan (3147m)", "fansipan-summit", 5,
                WaypointType.SUMMIT, "22.3031", "103.7764", 3147, 2, false,
                "Đỉnh cao nhất Đông Dương. Tầm nhìn 360° khi trời quang.",
                "Giới hạn 30-45 phút trên đỉnh. Gió mạnh, đảm bảo khách mặc đủ ấm.",
                false, false, false, false,
                WaterSourceType.NONE, null, null, null,
                "114", "Xuống cáp treo (30 phút) HOẶC quay về điểm 3000m.",
                "Bệnh viện Đa khoa Sa Pa", "25.0", false);

        TourWaypoint wp6 = wp(fansipan, "Bãi cắm trại 2800m (đêm 2)", "camp-2800-night2", 6,
                WaypointType.CAMP_SITE, "22.3089", "103.7635", 2800, 2, true,
                "Cùng bãi đêm 1 — nghỉ sau khi chinh phục đỉnh.",
                "Kiểm tra chấn thương. Nấu hot meal để hồi phục.",
                false, false, false, true,
                WaterSourceType.STREAM, "Nguồn nước như đêm 1.",
                AccommodationType.TENT_CAMPING, (short) 12,
                "0203 871 975", "Như đêm 1.",
                "Bệnh viện Đa khoa Sa Pa", "18.0", false);

        TourWaypoint wp7 = wp(fansipan, "Cổng Trạm Tôn (kết thúc)", "tram-ton-finish", 7,
                WaypointType.TRAILHEAD, "22.3417", "103.7753", 1900, 3, true,
                "Điểm kết thúc hành trình. Lên xe về Sa Pa.",
                "Kiểm tra thiết bị thuê. Ai bị thương ghi vào incident_log.",
                true, true, true, false,
                WaterSourceType.VILLAGE_TAP, "Vòi nước sạch tại cổng.",
                null, null,
                "0203 871 975", null,
                "Bệnh viện Đa khoa Sa Pa", "8.0", false);

        log.info("[Seed] 7 waypoints for Fansipan created.");

        // ── Daily Itinerary ────────────────────────────────────────────────────
        TourDailyItinerary day1 = itineraryRepository.save(TourDailyItinerary.builder()
                .tour(fansipan).dayNumber((short) 1)
                .dayTitle("Ngày 1: Trạm Tôn → Bãi cắm trại 2800m")
                .dayDescription("Xuyên rừng nguyên sinh Hoàng Liên từ 1900m lên 2800m.")
                .startWaypoint(wp1).endWaypoint(wp3).overnightWaypoint(wp3)
                .distanceKm(new BigDecimal("7.5")).elevationGainM(900).elevationLossM(0)
                .walkingHoursMin(new BigDecimal("6.0")).walkingHoursMax(new BigDecimal("8.0"))
                .dayDifficulty(DifficultyLevel.MODERATE)
                .suggestedStartTime(LocalTime.of(6, 0)).suggestedEndTime(LocalTime.of(15, 0))
                .mealsIncluded(List.of(
                        Map.of("type", "breakfast", "location", "khách sạn Sa Pa (tự túc)"),
                        Map.of("type", "lunch",     "location", "Nhà Gianh 2200m"),
                        Map.of("type", "dinner",    "location", "tại bãi cắm trại 2800m")))
                .mealNotes("HDV nấu dinner tại trại. Hỏi dị ứng thực phẩm trước.")
                .overnightNotes("Cắm trại cạnh suối. Giữ ấm sau hoàng hôn, nhiệt độ xuống 10-12°C.")
                .safetyNotes("Đoạn dốc 40 độ sau km 4, dài 500m. Dùng gậy, từng bước.").build());

        TourDailyItinerary day2 = itineraryRepository.save(TourDailyItinerary.builder()
                .tour(fansipan).dayNumber((short) 2)
                .dayTitle("Ngày 2: Chinh phục đỉnh Fansipan 3147m")
                .dayDescription("Ngày quyết định. Xuất phát sáng sớm để lên đỉnh lúc bình minh.")
                .startWaypoint(wp3).endWaypoint(wp6).overnightWaypoint(wp6)
                .distanceKm(new BigDecimal("6.0")).elevationGainM(347).elevationLossM(347)
                .walkingHoursMin(new BigDecimal("7.0")).walkingHoursMax(new BigDecimal("9.0"))
                .dayDifficulty(DifficultyLevel.HARD)
                .suggestedStartTime(LocalTime.of(4, 30)).suggestedEndTime(LocalTime.of(16, 0))
                .mealsIncluded(List.of(
                        Map.of("type", "breakfast", "location", "tại trại (bữa nhẹ trước 5:00)"),
                        Map.of("type", "lunch",     "location", "tại đỉnh hoặc trên đường xuống"),
                        Map.of("type", "dinner",    "location", "tại bãi cắm trại 2800m")))
                .mealNotes("Bữa sáng nhẹ: bánh mì + energy bar. Mang snack: chocolate, hạt, trái cây sấy.")
                .overnightNotes("Về trại trước 16:00. Nghỉ ngơi phục hồi.")
                .safetyNotes("QUAN TRỌNG: Sương mù < 20m sau điểm 3000m → dừng lại. Gió giật > 60km/h → hoãn.").build());

        TourDailyItinerary day3 = itineraryRepository.save(TourDailyItinerary.builder()
                .tour(fansipan).dayNumber((short) 3)
                .dayTitle("Ngày 3: Hạ sơn về Trạm Tôn")
                .dayDescription("Xuống núi qua lại tuyến ngày 1. Nhẹ nhàng hơn nhưng dễ đau gối.")
                .startWaypoint(wp6).endWaypoint(wp7)
                .distanceKm(new BigDecimal("7.5")).elevationGainM(0).elevationLossM(900)
                .walkingHoursMin(new BigDecimal("5.0")).walkingHoursMax(new BigDecimal("7.0"))
                .dayDifficulty(DifficultyLevel.MODERATE)
                .suggestedStartTime(LocalTime.of(7, 0)).suggestedEndTime(LocalTime.of(14, 0))
                .mealsIncluded(List.of(
                        Map.of("type", "breakfast", "location", "tại trại"),
                        Map.of("type", "lunch",     "location", "Nhà Gianh 2200m hoặc Trạm Tôn"),
                        Map.of("type", "dinner",    "location", "tự túc tại Sa Pa")))
                .mealNotes("Tháo trại sau breakfast. Bữa trưa tại Nhà Gianh nếu đến trước 13:00.")
                .safetyNotes("Xuống đúng kỹ thuật: gối hơi co, gót chân chạm trước. Dùng gậy bắt buộc.").build());

        log.info("[Seed] 3 daily itinerary entries for Fansipan created.");

        // ── Itinerary Waypoints ────────────────────────────────────────────────
        iw(day1, wp1, 1, true,  "Tập kết, kiểm tra thiết bị, xuất phát", "06:00");
        iw(day1, wp2, 2, true,  "Nghỉ trưa 45 phút, điểm danh, lấy nước", "10:30");
        iw(day1, wp3, 3, true,  "Dựng lều, nấu ăn, nghỉ ngơi", "14:30");
        iw(day2, wp3, 1, true,  "Xuất phát với đèn đầu", "04:30");
        iw(day2, wp4, 2, true,  "Điểm kiểm tra an toàn — đánh giá thời tiết trước khi tiếp tục", "07:00");
        iw(day2, wp5, 3, true,  "Chụp ảnh, nghỉ 30-45 phút, xuống ngay", "09:00");
        iw(day2, wp4, 4, true,  "Điểm nghỉ trên đường xuống", "11:30");
        iw(day2, wp6, 5, true,  "Về trại, nghỉ ngơi phục hồi", "14:30");
        iw(day3, wp6, 1, true,  "Tháo lều sau breakfast", "07:00");
        iw(day3, wp2, 2, true,  "Nghỉ trưa tùy chọn", "10:30");
        iw(day3, wp7, 3, true,  "Kết thúc, kiểm tra thiết bị thuê", "13:30");

        log.info("[Seed] Itinerary waypoints for Fansipan created.");
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Departures
    // ────────────────────────────────────────────────────────────────────────────

    private TourDeparture seedDeparture(
            Tour tour, String depDate, String retDate, String cutDate,
            String price, short maxGroup, short bookedSlots, boolean allowJoin,
            String meetingPoint, String weatherSummary, String weatherIcon,
            short tempMin, short tempMax, DepartureStatus status, Guide leadGuide) {

        TourDeparture dep = departureRepository.save(TourDeparture.builder()
                .tour(tour)
                .departureDate(LocalDate.parse(depDate))
                .returnDate(LocalDate.parse(retDate))
                .cutoffDate(LocalDate.parse(cutDate))
                .pricePerPerson(new BigDecimal(price))
                .maxGroupSize(maxGroup).minGroupSize((short) 2)
                .bookedSlots(bookedSlots).allowJoinTour(allowJoin)
                .meetingPoint(meetingPoint)
                .weatherSummary(weatherSummary).weatherIcon(weatherIcon)
                .tempMinC(tempMin).tempMaxC(tempMax)
                .weatherUpdatedAt(LocalDateTime.now())
                .status(status).build());

        departureGuideRepository.save(DepartureGuide.builder()
                .id(new DepartureGuideId(dep.getId(), leadGuide.getId()))
                .departure(dep).guide(leadGuide)
                .role(GuideRoleInTour.LEAD)
                .confirmedAt(LocalDateTime.now()).build());

        return dep;
    }

    private void updateCompletedDeparture(TourDeparture dep) {
        dep.setActualStartAt(LocalDateTime.of(2025, 4, 10, 6, 15, 0));
        dep.setActualEndAt(LocalDateTime.of(2025, 4, 12, 16, 30, 0));
        dep.setActualParticipants((short) 10);
        dep.setDebriefNotes("Chuyến đi thuận lợi, thời tiết đẹp. Cả đoàn lên đỉnh thành công.");
        dep.setIncidentLog(List.of(Map.of(
                "time",       "2025-04-11T09:30:00+07:00",
                "type",       "minor_injury",
                "note",       "Thành viên bị đau gối phải khi xuống dốc",
                "handled_by", "son.nguyen@trailviet.vn",
                "resolved",   true)));
        departureRepository.save(dep);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Weather Daily (Fansipan departure 2025-06-02)
    // ────────────────────────────────────────────────────────────────────────────

    private void seedWeatherForDeparture(TourDeparture dep, Tour fansipan) {
        TourDailyItinerary it1 = itineraryRepository
                .findByTourIdAndDayNumber(fansipan.getId(), (short) 1).orElse(null);
        TourDailyItinerary it2 = itineraryRepository
                .findByTourIdAndDayNumber(fansipan.getId(), (short) 2).orElse(null);
        TourDailyItinerary it3 = itineraryRepository
                .findByTourIdAndDayNumber(fansipan.getId(), (short) 3).orElse(null);

        weatherDailyRepository.save(DepartureWeatherDaily.builder()
                .departure(dep).dayNumber((short) 1)
                .forecastDate(LocalDate.parse("2025-06-02")).itinerary(it1)
                .locationLabel("Trạm Tôn (1900m) → Bãi cắm trại (2800m)").elevationM(2200)
                .weatherSummary("Nắng sáng sớm, mây tích chiều tối").weatherIcon("partly-cloudy")
                .tempMinC((short) 14).tempMaxC((short) 22)
                .feelsLikeMinC((short) 10).feelsLikeMaxC((short) 18)
                .precipitationMm(new BigDecimal("1.5")).precipitationProb((short) 20)
                .windSpeedKmh((short) 15).windGustKmh((short) 25)
                .humidityPct((short) 72).visibilityKm(new BigDecimal("8.0")).uvIndex((short) 7)
                .warningLevel(WarningLevel.INFO).dataSource("openweathermap").build());

        weatherDailyRepository.save(DepartureWeatherDaily.builder()
                .departure(dep).dayNumber((short) 2)
                .forecastDate(LocalDate.parse("2025-06-03")).itinerary(it2)
                .locationLabel("Bãi cắm trại (2800m) → Đỉnh Fansipan (3147m)").elevationM(3147)
                .weatherSummary("Sáng sớm sương mù dày, quang dần sau 8h. Gió mạnh trên đỉnh.").weatherIcon("foggy")
                .tempMinC((short) 8).tempMaxC((short) 15)
                .feelsLikeMinC((short) 4).feelsLikeMaxC((short) 10)
                .precipitationMm(BigDecimal.ZERO).precipitationProb((short) 10)
                .windSpeedKmh((short) 30).windGustKmh((short) 45)
                .humidityPct((short) 85).visibilityKm(new BigDecimal("1.5")).uvIndex((short) 9)
                .weatherWarning("Sương mù sáng, tầm nhìn < 50m trước 7:00. Gió giật 45km/h trên đỉnh — cần áo gió.")
                .warningLevel(WarningLevel.CAUTION).dataSource("openweathermap").build());

        weatherDailyRepository.save(DepartureWeatherDaily.builder()
                .departure(dep).dayNumber((short) 3)
                .forecastDate(LocalDate.parse("2025-06-04")).itinerary(it3)
                .locationLabel("Bãi cắm trại (2800m) → Trạm Tôn (1900m)").elevationM(1900)
                .weatherSummary("Nắng đẹp suốt ngày, xuống núi thuận lợi").weatherIcon("sunny")
                .tempMinC((short) 12).tempMaxC((short) 20)
                .feelsLikeMinC((short) 10).feelsLikeMaxC((short) 18)
                .precipitationMm(BigDecimal.ZERO).precipitationProb((short) 5)
                .windSpeedKmh((short) 10).windGustKmh((short) 20)
                .humidityPct((short) 60).visibilityKm(new BigDecimal("12.0")).uvIndex((short) 6)
                .warningLevel(WarningLevel.INFO).dataSource("openweathermap").build());

        log.info("[Seed] 3 daily weather entries for departure 2025-06-02 created.");
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Bookings + Reviews
    // ────────────────────────────────────────────────────────────────────────────

    private void seedBookingsAndReviews(SeedUsers users, Tour fansipan,
                                         TourDeparture dep10, TourDeparture dep5,
                                         TourDeparture dep6) {
        // Booking 1 — Hoa đi Fansipan (completed)
        Booking b1 = bookingRepository.save(Booking.builder()
                .bookingCode("TV-20250410-0001")
                .user(users.hoa()).departure(dep10)
                .numParticipants((short) 2)
                .priceSnapshot(new BigDecimal("2600000"))
                .subtotalTour(new BigDecimal("5200000"))
                .subtotalEquipment(new BigDecimal("460000"))
                .totalPrice(new BigDecimal("5660000"))
                .status(BookingStatus.COMPLETED)
                .paidAt(LocalDateTime.now().minusDays(50)).build());

        // Booking 2 — Khiêm đi Tà Năng (confirmed)
        bookingRepository.save(Booking.builder()
                .bookingCode("TV-20250614-0001")
                .user(users.khiem()).departure(dep5)
                .numParticipants((short) 1)
                .priceSnapshot(new BigDecimal("1950000"))
                .subtotalTour(new BigDecimal("1950000"))
                .subtotalEquipment(new BigDecimal("230000"))
                .totalPrice(new BigDecimal("2180000"))
                .status(BookingStatus.CONFIRMED)
                .paidAt(LocalDateTime.now().minusDays(5)).build());

        // Booking 3 — Hoa đi Mã Pí Lèng (confirmed, join tour)
        bookingRepository.save(Booking.builder()
                .bookingCode("TV-20250621-0001")
                .user(users.hoa()).departure(dep6)
                .numParticipants((short) 1)
                .priceSnapshot(new BigDecimal("1500000"))
                .subtotalTour(new BigDecimal("1500000"))
                .totalPrice(new BigDecimal("1500000"))
                .isJoinTour(true).status(BookingStatus.CONFIRMED)
                .paidAt(LocalDateTime.now().minusDays(3)).build());

        log.info("[Seed] 3 bookings created.");

        // Review của Hoa cho chuyến Fansipan
        reviewRepository.save(Review.builder()
                .user(users.hoa()).booking(b1)
                .tour(fansipan).departure(dep10).guide(users.son())
                .overallRating((short) 5).guideRating((short) 5)
                .sceneryRating((short) 5).safetyRating((short) 5)
                .valueRating((short) 5).difficultyRating((short) 4)
                .title("Trải nghiệm tuyệt vời, vượt mọi kỳ vọng!")
                .comment("Chuyến đi hoàn hảo! Anh Sơn xử lý rất chuyên nghiệp khi có thành viên bị đau gối.")
                .isApproved(true).build());

        log.info("[Seed] 1 review created.");
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ────────────────────────────────────────────────────────────────────────────

    private User createUser(String email, String phone, String pw, boolean isAdmin) {
        return userRepository.save(User.builder()
                .email(email).phone(phone).passwordHash(pw)
                .isVerified(true).isActive(true).isAdmin(isAdmin).build());
    }

    private void createCustomer(User user, String fullName, String dob, FitnessLevel fitness) {
        customerRepository.save(Customer.builder()
                .user(user).fullName(fullName)
                .dateOfBirth(LocalDate.parse(dob))
                .nationality("Việt Nam").fitnessLevel(fitness).build());
    }

    private Guide createGuide(User user, String displayName, String bio, short expYears,
                               List<Map<String, Object>> certs,
                               List<String> langs, List<String> specs,
                               String province, String rating, int totalReviews, int toursLed) {
        return guideRepository.save(Guide.builder()
                .user(user).displayName(displayName).bio(bio)
                .experienceYears(expYears).certifications(certs)
                .languages(langs).specializations(specs).homeProvince(province)
                .avgRating(new BigDecimal(rating))
                .totalReviews(totalReviews).totalToursLed(toursLed)
                .idCardVerified(true)
                .profileApprovedAt(LocalDateTime.now().minusMonths(6)).build());
    }

    /** Tạo TourWaypoint — short signature để tránh lặp code. */
    private TourWaypoint wp(Tour tour, String name, String slug, int seq,
                             WaypointType type, String lat, String lng, int elev,
                             int dayNum, boolean isDayEnd,
                             String desc, String guideNotes,
                             boolean toilet, boolean shelter, boolean signal, boolean firstAid,
                             WaterSourceType water, String waterNotes,
                             AccommodationType accom, Short campCap,
                             String emgPhone, String evacNotes, String hospital,
                             String hospDist, boolean heli) {
        return tourWaypointRepository.save(TourWaypoint.builder()
                .tour(tour).name(name).slug(slug).sequenceOrder((short) seq)
                .waypointType(type)
                .lat(new BigDecimal(lat)).lng(new BigDecimal(lng)).elevationM(elev)
                .dayNumber((short) dayNum).isDayEnd(isDayEnd)
                .description(desc).notesForGuide(guideNotes)
                .hasToilet(toilet).hasShelter(shelter)
                .hasPhoneSignal(signal).hasFirstAid(firstAid)
                .waterSource(water).waterNotes(waterNotes)
                .accommodation(accom).campsiteCapacity(campCap)
                .emergencyPhone(emgPhone).evacuationRouteNotes(evacNotes)
                .nearestHospital(hospital)
                .hospitalDistanceKm(hospDist != null ? new BigDecimal(hospDist) : null)
                .helicopterLanding(heli).build());
    }

    /** Tạo ItineraryWaypoint — UUID PK tự sinh, cho phép cùng waypoint xuất hiện nhiều lần/ngày. */
    private void iw(TourDailyItinerary itinerary, TourWaypoint waypoint,
                    int order, boolean mandatory, String notes, String arrival) {
        itineraryWaypointRepository.save(ItineraryWaypoint.builder()
                .itinerary(itinerary).waypoint(waypoint)
                .visitOrder((short) order).isMandatory(mandatory)
                .visitNotes(notes).estimatedArrival(LocalTime.parse(arrival)).build());
    }
}

