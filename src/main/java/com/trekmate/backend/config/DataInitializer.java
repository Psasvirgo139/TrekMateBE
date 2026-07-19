package com.trekmate.backend.config;

import com.trekmate.backend.model.*;
import com.trekmate.backend.model.embeddable.DepartureGuideId;
import com.trekmate.backend.model.enums.*;
import com.trekmate.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;

import java.util.ArrayList;
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
    private final EquipmentRepository         equipmentRepository;
    private final TourImageRepository         tourImageRepository;
    private final PasswordEncoder             passwordEncoder;
    private final LocationRepository          locationRepository;
    private final TourAttributeRepository     tourAttributeRepository;
    private final EquipmentRentalRepository   equipmentRentalRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    // ─── Wrapper giữ các entity đã seed để truyền giữa các bước ───────────────
    private record SeedUsers(
            User admin,
            List<User> customers,
            Guide son, Guide mai, Guide anh) {}

    private record SeedTours(List<Tour> tours) {}

    // ───────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void run(String... args) {
        try {
            jdbcTemplate.execute("ALTER TABLE tours DROP COLUMN IF EXISTS excludes");
            jdbcTemplate.execute("ALTER TABLE tours DROP COLUMN IF EXISTS includes");
            jdbcTemplate.execute("ALTER TABLE tours DROP COLUMN IF EXISTS highlights");
            jdbcTemplate.execute("ALTER TABLE tours DROP COLUMN IF EXISTS requirements");
            log.info("[DataInitializer] Đã dọn dẹp các cột thừa (excludes, includes, highlights, requirements) trong bảng tours thành công!");
        } catch (Exception e) {
            log.warn("[DataInitializer] Không thể dọn dẹp cột thừa trong bảng tours: {}", e.getMessage());
        }

        try {
            Integer hasIdCol = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'guides' AND column_name = 'id'",
                Integer.class
            );
            if (hasIdCol != null && hasIdCol > 0) {
                log.info("[DataInitializer] Phát hiện cột id cũ trong bảng guides. Tiến hành di chuyển khoá chính sang user_id...");
                
                // 1. Drop constraints first to avoid violations during update
                jdbcTemplate.execute("ALTER TABLE departure_guides DROP CONSTRAINT IF EXISTS fkv9i7wwx3s3xpmnrjhv9s1msq");
                jdbcTemplate.execute("ALTER TABLE reviews DROP CONSTRAINT IF EXISTS fkbec51k4am7j0j64piv7gl42l7");
                jdbcTemplate.execute("ALTER TABLE guide_ratings DROP CONSTRAINT IF EXISTS fk7q038v2ptud4kd2xhx7pdh2qq");

                // 2. Now update values safely without active FK constraints
                jdbcTemplate.execute("UPDATE departure_guides dg SET guide_id = g.user_id FROM guides g WHERE dg.guide_id = g.id");
                jdbcTemplate.execute("UPDATE reviews r SET guide_id = g.user_id FROM guides g WHERE r.guide_id = g.id");
                jdbcTemplate.execute("UPDATE guide_ratings gr SET guide_id = g.user_id FROM guides g WHERE gr.guide_id = g.id");
                
                // 3. Drop primary key and old column
                jdbcTemplate.execute("ALTER TABLE guides DROP CONSTRAINT IF EXISTS guides_pkey");
                jdbcTemplate.execute("ALTER TABLE guides DROP COLUMN IF EXISTS id");
                
                // 4. Recreate primary key on user_id
                jdbcTemplate.execute("ALTER TABLE guides ADD CONSTRAINT guides_pkey PRIMARY KEY (user_id)");
                
                // 5. Recreate foreign keys pointing to user_id
                jdbcTemplate.execute("ALTER TABLE departure_guides ADD CONSTRAINT fkv9i7wwx3s3xpmnrjhv9s1msq FOREIGN KEY (guide_id) REFERENCES guides(user_id)");
                jdbcTemplate.execute("ALTER TABLE reviews ADD CONSTRAINT fkbec51k4am7j0j64piv7gl42l7 FOREIGN KEY (guide_id) REFERENCES guides(user_id)");
                jdbcTemplate.execute("ALTER TABLE guide_ratings ADD CONSTRAINT fk7q038v2ptud4kd2xhx7pdh2qq FOREIGN KEY (guide_id) REFERENCES guides(user_id)");
                
                log.info("[DataInitializer] Di chuyển khoá chính bảng guides và cập nhật các khoá ngoại thành công!");
            }
        } catch (Exception e) {
            log.error("[DataInitializer] Lỗi khi xử lý cấu trúc bảng guides: {}", e.getMessage());
        }

        if (userRepository.count() > 0) {
            log.info("[DataInitializer] Database đã có dữ liệu — bỏ qua seed.");
            // Tự động seed equipment nếu bị trống dữ liệu thiết bị
            if (equipmentRepository.count() == 0) {
                log.info("[DataInitializer] Phát hiện bảng equipment trống — tự động seed equipment cho các category hiện có...");
                List<EquipmentCategory> categories = equipmentCategoryRepository.findAll();
                for (EquipmentCategory cat : categories) {
                    equipmentRepository.save(Equipment.builder()
                            .category(cat)
                            .name("Trang thiết bị " + cat.getName() + " cao cấp")
                            .description("Mô tả cho trang thiết bị " + cat.getName())
                            .brand("Naturehike")
                            .model("NH20ZP015")
                            .pricePerDay(new BigDecimal("50000"))
                            .depositAmount(new BigDecimal("200000"))
                            .totalStock((short) 50)
                            .availableStock((short) 50)
                            .condition(EquipmentCondition.GOOD)
                            .isActive(true)
                            .build());
                }
            }
            // Seed tour image nếu cần          
            updateSeededTourImages();
            // Tự động dịch chuyển các ngày khởi hành cũ trong quá khứ lên tương lai để dữ liệu demo luôn mới
            List<TourDeparture> allDeps = departureRepository.findAll();
            LocalDate today = LocalDate.now();
            boolean updatedAny = false;
            for (TourDeparture dep : allDeps) {
                if (dep.getDepartureDate() != null && dep.getDepartureDate().isBefore(today) && dep.getStatus() == DepartureStatus.OPEN) {
                    long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(dep.getDepartureDate(), today) + 10;
                    dep.setDepartureDate(dep.getDepartureDate().plusDays(daysDiff));
                    if (dep.getReturnDate() != null) {
                        dep.setReturnDate(dep.getReturnDate().plusDays(daysDiff));
                    }
                    if (dep.getCutoffDate() != null) {
                        dep.setCutoffDate(dep.getCutoffDate().plusDays(daysDiff));
                    }
                    departureRepository.save(dep);
                    updatedAny = true;
                }
            }
            if (updatedAny) {
                log.info("[DataInitializer] Đã cập nhật ngày của các đợt khởi hành cũ lên tương lai để test.");
            }
            ensureFutureDeparturesForTours();
            updateTourRatingAndReviewsStats();
            return;
        }
        log.info("[DataInitializer] Database trống — bắt đầu seed dữ liệu...");

        seedLocations();
        seedEquipmentCategories();

        SeedUsers users = seedUsers();
        SeedTours tours = seedTours(users.admin());

        // Tour 1 (Fansipan) dùng lịch trình viết tay chi tiết có sẵn
        seedFansipanWaypointsAndItinerary(tours.tours().get(0));

        // Các tour còn lại (2 đến 9) dùng cơ chế sinh động Waypoints & Itinerary
        for (int i = 1; i < tours.tours().size(); i++) {
            seedTourItineraryAndWaypoints(tours.tours().get(i));
        }

        // Seed ảnh tour cho toàn bộ 9 tour
        seedTourImages(tours.tours(), users.admin().getId());

        // Seed đợt khởi hành, booking đầy đoàn, đánh giá, thời tiết và thuê đồ lịch sử
        seedDeparturesBookingsReviewsWeatherAndRentals(users, tours.tours());

        ensureFutureDeparturesForTours();
        updateTourRatingAndReviewsStats();
        log.info("[DataInitializer] Seed hoàn tất.");
    }

    private void seedTourImages(List<Tour> tours, java.util.UUID adminId) {
        String[][] imagesData = {
            // Fansipan
            {"https://images.unsplash.com/photo-1528127269322-539801943592?auto=format&fit=crop&w=1200&q=80", "Bình minh trên đỉnh Fansipan", "Fansipan summit sunrise"},
            {"https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=1200&q=80", "Rừng nguyên sinh Hoàng Liên Sơn", "Hoang Lien Son forest"},
            {"https://images.unsplash.com/photo-1589308078059-be1415eab4c3?auto=format&fit=crop&w=1200&q=80", "Mây che phủ lũng núi Sapa", "Sapa misty valleys"},
            {"https://images.unsplash.com/photo-1501854140801-50d01698950b?auto=format&fit=crop&w=1200&q=80", "Bình minh Fansipan xanh ngút ngàn", "Fansipan peak views"},
            {"https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=1200&q=80", "Hùng vĩ dãy Hoàng Liên Sơn", "Hoang Lien mountain range"},
            
            // Ta Nang
            {"https://images.unsplash.com/photo-1470240731273-7821a6eeb6bd?auto=format&fit=crop&w=1200&q=80", "Thảo nguyên Tà Năng Phan Dũng", "Ta Nang Phan Dung grasslands"},
            {"https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=1200&q=80", "Thảo nguyên xanh mát rượi", "Ta Nang lush valley"},
            {"https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?auto=format&fit=crop&w=1200&q=80", "Rừng thông Tà Năng mờ sương", "Ta Nang misty pines"},
            {"https://images.unsplash.com/photo-1500627869374-13cd993b1115?auto=format&fit=crop&w=1200&q=80", "Đón bình minh tại đồi cỏ", "Ta Nang hill sunrise"},
            {"https://images.unsplash.com/photo-1434064511983-18c6dae20ed5?auto=format&fit=crop&w=1200&q=80", "Chiều hoàng hôn buông xuống Tà Năng", "Ta Nang twilight sky"},

            // Ma Pi Leng
            {"https://images.unsplash.com/photo-1605538032432-a9f0c8d9baac?auto=format&fit=crop&w=1200&q=80", "Hùng vĩ Mã Pí Lèng Hà Giang", "Ma Pi Leng pass"},
            {"https://images.unsplash.com/photo-1547036967-23d11aacaee0?auto=format&fit=crop&w=1200&q=80", "Sông Nho Quế uốn lượn hiền hòa", "Nho Que river canyon"},
            {"https://images.unsplash.com/photo-1504280390367-361c6d9f38f4?auto=format&fit=crop&w=1200&q=80", "Con đèo Mã Pí Lèng hiểm trở", "Ma Pi Leng winding pass"},
            {"https://images.unsplash.com/photo-1486915309851-b0cc1f8a0084?auto=format&fit=crop&w=1200&q=80", "Hoàng hôn Mã Pí Lèng", "Ma Pi Leng sunset"},
            {"https://images.unsplash.com/photo-1519681393784-d120267933ba?auto=format&fit=crop&w=1200&q=80", "Đồi đá cao nguyên Hà Giang", "Dong Van karst plateau"},

            // Cao Bang
            {"https://images.unsplash.com/photo-1508739773434-c26b3d09e071?auto=format&fit=crop&w=1200&q=80", "Thác Bản Giốc lung linh ánh nắng", "Ban Gioc waterfall"},
            {"https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?auto=format&fit=crop&w=1200&q=80", "Nét hoang sơ thung lũng Cao Bằng", "Cao Bang landscape"},
            {"https://images.unsplash.com/photo-1441974231531-c6227db76b6e?auto=format&fit=crop&w=1200&q=80", "Dòng nước xanh mát lượn lách", "Cao Bang river valley"},
            {"https://images.unsplash.com/photo-1513836279014-a89f7a76ae86?auto=format&fit=crop&w=1200&q=80", "Cầu tre nhỏ bắc qua suối", "Cao Bang rustic bridge"},
            {"https://images.unsplash.com/photo-1426604966848-d7adac402bff?auto=format&fit=crop&w=1200&q=80", "Núi non trùng điệp Cao Bằng", "Cao Bang green hills"},

            // Bach Ma
            {"https://images.unsplash.com/photo-1542224566-6e85f2e6772f?auto=format&fit=crop&w=1200&q=80", "Vọng Hải Đài ngập trong nắng", "Vong Hai Dai tower view"},
            {"https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=1200&q=80", "Hồ nước xanh như ngọc tại Ngũ Hồ", "Bach Ma Ngu Ho lakes"},
            {"https://images.unsplash.com/photo-1432406776043-6c76db202812?auto=format&fit=crop&w=1200&q=80", "Thác Đỗ Quyên cuồn cuộn đổ", "Bach Ma Do Quyen waterfall"},
            {"https://images.unsplash.com/photo-1473448912268-2022ce9509d8?auto=format&fit=crop&w=1200&q=80", "Rừng nguyên sinh Bạch Mã", "Bach Ma rainforest"},
            {"https://images.unsplash.com/photo-1502082553048-f009c37129b9?auto=format&fit=crop&w=1200&q=80", "Những cây cổ thụ bám rễ đá", "Bach Ma ancient forest"},

            // Pu Luong
            {"https://images.unsplash.com/photo-1504280390367-361c6d9f38f4?auto=format&fit=crop&w=1200&q=80", "Bản làng Thái bình yên Pù Luông", "Pu Luong valley view"},
            {"https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1200&q=80", "Bình minh trên những thửa ruộng bậc thang", "Pu Luong rice terraces"},
            {"https://images.unsplash.com/photo-1518495973542-4542c06a5843?auto=format&fit=crop&w=1200&q=80", "Nhà sàn ẩn hiện giữa sương mù", "Pu Luong homestay"},
            {"https://images.unsplash.com/photo-1475924156734-496f6cac6ec1?auto=format&fit=crop&w=1200&q=80", "Dòng suối mát len lỏi trong bản", "Pu Luong stream"},
            {"https://images.unsplash.com/photo-1469474968028-56623f02e42e?auto=format&fit=crop&w=1200&q=80", "Góc nhìn từ đỉnh núi Pù Luông", "Pu Luong peak view"},

            // Chu Yang Sin
            {"https://images.unsplash.com/photo-1511497584788-876760111969?auto=format&fit=crop&w=1200&q=80", "Chư Yang Sin hoang sơ kỳ bí", "Chu Yang Sin mountain"},
            {"https://images.unsplash.com/photo-1513836279014-a89f7a76ae86?auto=format&fit=crop&w=1200&q=80", "Rừng lá kim độc đáo trên tuyến", "Chu Yang Sin pine trees"},
            {"https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=1200&q=80", "Đoạn dốc thẳng đứng đá phủ rêu", "Chu Yang Sin mossy trail"},
            {"https://images.unsplash.com/photo-1501854140801-50d01698950b?auto=format&fit=crop&w=1200&q=80", "Đỉnh núi mờ sương Chư Yang Sin", "Chu Yang Sin peak foggy"},
            {"https://images.unsplash.com/photo-1473448912268-2022ce9509d8?auto=format&fit=crop&w=1200&q=80", "Dòng thác ẩn mình giữa đại ngàn", "Chu Yang Sin waterfall"},

            // Lao Than
            {"https://images.unsplash.com/photo-1533105079780-92b9be482077?auto=format&fit=crop&w=1200&q=80", "Biển mây Y Tý Lao Thẩn", "Lao Than clouds sea"},
            {"https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=1200&q=80", "Đón những tia nắng đầu ngày trên đỉnh", "Lao Than summit sunrise"},
            {"https://images.unsplash.com/photo-1589308078059-be1415eab4c3?auto=format&fit=crop&w=1200&q=80", "Lán nghỉ lưng chừng núi Lao Thẩn", "Lao Than campsite"},
            {"https://images.unsplash.com/photo-1472214222541-d510753a4907?auto=format&fit=crop&w=1200&q=80", "Đồng cỏ vàng dưới chân đỉnh", "Lao Than dry grass hill"},
            {"https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=1200&q=80", "Hoàng hôn buông giữa mây ngàn", "Lao Than sunset clouds"},

            // Nam Cat Tien
            {"https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=1200&q=80", "Bàu Sấu - Trái tim rừng Nam Cát Tiên", "Bau Sau ramsar wetland"},
            {"https://images.unsplash.com/photo-1546182990-dffeafbe841d?auto=format&fit=crop&w=1200&q=80", "Động vật hoang dã đi ăn đêm", "Nam Cat Tien nocturnal deer"},
            {"https://images.unsplash.com/photo-1470240731273-7821a6eeb6bd?auto=format&fit=crop&w=1200&q=80", "Cầu gỗ đầm lầy tại Bàu Sấu", "Bau Sau wooden boardwalk"},
            {"https://images.unsplash.com/photo-1502082553048-f009c37129b9?auto=format&fit=crop&w=1200&q=80", "Cây tung cổ thụ hàng trăm năm", "Nam Cat Tien Tung tree"},
            {"https://images.unsplash.com/photo-1434064511983-18c6dae20ed5?auto=format&fit=crop&w=1200&q=80", "Rừng xanh bao la Nam Cát Tiên", "Nam Cat Tien tropical forest"}
        };

        int imgIndex = 0;
        for (Tour tour : tours) {
            for (short i = 1; i <= 5; i++) {
                String[] img = imagesData[imgIndex++];
                tourImageRepository.save(TourImage.builder()
                        .tour(tour)
                        .imageUrl(img[0])
                        .caption(img[1])
                        .altText(img[2])
                        .isCover(i == 1)
                        .sortOrder(i)
                        .uploadedBy(adminId)
                        .build());
            }
        }
        log.info("[Seed] Tour images created.");
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Equipment Categories & 15 Equipments Seeding
    // ────────────────────────────────────────────────────────────────────────────

    private void seedEquipmentCategories() {
        String[][] categoriesData = {
            {"Lều trại",          "camping-tent",      "tent",     "1"},
            {"Túi ngủ",           "sleeping-bag",      "sleep",    "2"},
            {"Gậy & Dụng cụ leo", "trekking-poles",    "stick",    "3"},
            {"Balo",              "backpack",          "backpack", "4"},
            {"Quần áo kỹ thuật",  "technical-apparel", "jacket",   "5"},
            {"Thiết bị an toàn",  "safety-gear",       "helmet",   "6"},
            {"Đèn & Điện",        "lighting",          "torch",    "7"},
        };
        List<EquipmentCategory> categories = new ArrayList<>();
        for (String[] c : categoriesData) {
            EquipmentCategory cat = equipmentCategoryRepository.save(EquipmentCategory.builder()
                    .name(c[0]).slug(c[1]).icon(c[2])
                    .sortOrder(Short.parseShort(c[3])).build());
            categories.add(cat);
        }

        // Seed exactly 15 equipments: 10 active, 5 inactive (retired)
        // Category 1: Lều trại
        seedEquipment(categories.get(0), "Lều 2 người Naturehike", "Naturehike", "NH20ZP015", "50000", "200000", (short)50, EquipmentCondition.GOOD, true);
        seedEquipment(categories.get(0), "Lều 4 người Coleman", "Coleman", "CL-4P", "80000", "300000", (short)30, EquipmentCondition.EXCELLENT, true);
        seedEquipment(categories.get(0), "Lều đơn siêu nhẹ Naturehike", "Naturehike", "NH-SINGLE", "40000", "150000", (short)10, EquipmentCondition.RETIRED, false);

        // Category 2: Túi ngủ
        seedEquipment(categories.get(1), "Túi ngủ lông vũ Naturehike", "Naturehike", "NH19D015", "30000", "100000", (short)40, EquipmentCondition.EXCELLENT, true);
        seedEquipment(categories.get(1), "Túi ngủ bông ấm áp TrailViet", "TrailViet", "TV-SLEEP", "20000", "80000", (short)60, EquipmentCondition.GOOD, true);

        // Category 3: Gậy & Dụng cụ leo
        seedEquipment(categories.get(2), "Gậy trekking Carbon Naturehike", "Naturehike", "NH17D012-D", "15000", "50000", (short)100, EquipmentCondition.GOOD, true);
        seedEquipment(categories.get(2), "Gậy trekking nhôm Đăng Sơn", "Đăng Sơn", "DS-POLE", "10000", "30000", (short)20, EquipmentCondition.RETIRED, false);

        // Category 4: Balo
        seedEquipment(categories.get(3), "Balo trekking Deuter 50L", "Deuter", "ACT-LITE-50", "60000", "250000", (short)25, EquipmentCondition.EXCELLENT, true);
        seedEquipment(categories.get(3), "Balo trekking Osprey 65L", "Osprey", "AETHER-65", "70000", "300000", (short)20, EquipmentCondition.GOOD, true);
        seedEquipment(categories.get(3), "Balo nhỏ dã ngoại 20L", "Quechua", "QC-20L", "15000", "50000", (short)15, EquipmentCondition.RETIRED, false);

        // Category 5: Quần áo kỹ thuật
        seedEquipment(categories.get(4), "Áo khoác chống nước Gore-Tex", "The North Face", "TNF-GTX", "40000", "150000", (short)35, EquipmentCondition.GOOD, true);
        seedEquipment(categories.get(4), "Áo mưa bộ siêu nhẹ TrailViet", "TrailViet", "TV-RAIN", "10000", "30000", (short)10, EquipmentCondition.RETIRED, false);

        // Category 6: Thiết bị an toàn
        seedEquipment(categories.get(5), "Bộ sơ cứu y tế cá nhân", "FirstAidCo", "FA-BASIC", "10000", "30000", (short)150, EquipmentCondition.EXCELLENT, true);
        seedEquipment(categories.get(5), "Mũ bảo hiểm leo núi chuyên dụng", "Petzl", "METEOR", "25000", "100000", (short)40, EquipmentCondition.GOOD, true);

        // Category 7: Đèn & Điện
        seedEquipment(categories.get(6), "Đèn đầu siêu sáng Petzl", "Petzl", "TIKKA", "20000", "80000", (short)5, EquipmentCondition.RETIRED, false);

        log.info("[Seed] {} equipment categories and 15 sample equipments created", categoriesData.length);
    }

    private void seedEquipment(EquipmentCategory cat, String name, String brand, String model, String price, String deposit, short stock, EquipmentCondition cond, boolean active) {
        equipmentRepository.save(Equipment.builder()
                .category(cat)
                .name(name)
                .brand(brand)
                .model(model)
                .pricePerDay(new BigDecimal(price))
                .depositAmount(new BigDecimal(deposit))
                .totalStock(stock)
                .availableStock(stock)
                .condition(cond)
                .isActive(active)
                .build());
    }

    private void seedLocations() {
        if (locationRepository.count() > 0) {
            return;
        }
        List<String> defaultLocations = List.of(
            "Hà Nội", "Đà Nẵng", "TP. Hồ Chí Minh", "Vịnh Hạ Long", 
            "Sa Pa", "Hà Giang", "Đà Lạt", "Ninh Bình", "Phú Quốc"
        );
        for (String name : defaultLocations) {
            locationRepository.save(Location.builder()
                    .name(name)
                    .description("Địa điểm du lịch và trekking nổi tiếng " + name)
                    .build());
        }
        log.info("[Seed] {} default locations created.", defaultLocations.size());
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Users + Customers + Guides
    // ────────────────────────────────────────────────────────────────────────────

    private SeedUsers seedUsers() {
        String pw = passwordEncoder.encode("Test@1234");
        List<User> customersList = new ArrayList<>();

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
        customersList.add(hoa);

        User khiem = createUser("khiem@example.com", "0922222222", pw, false);
        createCustomer(khiem, "Phạm Gia Khiêm", "1995-09-30", FitnessLevel.BEGINNER);
        customersList.add(khiem);

        // ── Seed thêm 18 customer nữa để đạt tổng cộng 20 customer
        String[] firstNames = {"Trần", "Lê", "Phạm", "Hoàng", "Huỳnh", "Phan", "Vũ", "Võ", "Đặng", "Bùi", "Đỗ", "Hồ", "Ngô", "Dương", "Lý"};
        String[] middleNames = {"Văn", "Thị", "Hữu", "Đức", "Minh", "Thu", "Ngọc", "Gia", "Thanh", "Hoài", "Anh", "Xuân"};
        String[] lastNames = {"Anh", "Bình", "Chương", "Duy", "Dương", "Đạt", "Hải", "Khánh", "Linh", "Nam", "Phong", "Quân", "Sơn", "Thảo", "Trang", "Tuấn", "Vy", "Yến"};
        
        FitnessLevel[] levels = FitnessLevel.values();

        for (int i = 1; i <= 18; i++) {
            String email = "customer" + i + "@example.com";
            String phone = String.format("0933333%03d", i);
            String fullName = firstNames[i % firstNames.length] + " " + 
                              middleNames[(i * 3) % middleNames.length] + " " + 
                              lastNames[(i * 7) % lastNames.length];
            
            int birthYear = 1980 + (i * 7) % 26;
            String dob = birthYear + "-06-15";
            FitnessLevel fitness = levels[i % levels.length];

            User customerUser = createUser(email, phone, pw, false);
            createCustomer(customerUser, fullName, dob, fitness);
            customersList.add(customerUser);
        }

        // ── Admin
        User admin = createUser("admin@trailviet.vn", "0800000001", pw, true);

        log.info("[Seed] Users, customers, guides created.");
        return new SeedUsers(admin, customersList, son, mai, anh);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Tours
    // ────────────────────────────────────────────────────────────────────────────

    private SeedTours seedTours(User admin) {
        List<Tour> toursList = new ArrayList<>();

        // Tour 1: Fansipan
        Tour fansipan = Tour.builder()
                .title("Chinh phục Fansipan — Nóc nhà Đông Dương")
                .slug("fansipan-summit")
                .shortDescription("Hành trình chinh phục đỉnh Fansipan 3,147m hùng vĩ.")
                .difficulty(DifficultyLevel.HARD)
                .durationDays((short) 3).durationNights((short) 2)
                .distanceKm(new BigDecimal("19.0")).maxElevationM(3147)
                .startLocation("Sa Pa, Lào Cai").endLocation("Sa Pa, Lào Cai")
                .status(TourStatus.ACTIVE).createdBy(admin.getId()).build();
        mapAttributes(fansipan, 
                List.of("Đỉnh cao nhất Đông Dương 3147m","Rừng nguyên sinh Hoàng Liên","Sunrise trên mây"),
                List.of("HDV chuyên nghiệp","Lều trại","Bữa ăn trên đường","Cứu thương cơ bản"),
                List.of("Vé cáp treo","Bảo hiểm du lịch","Chi phí cá nhân"),
                List.of("Sức khoẻ tốt, leo bộ 8h/ngày","Kinh nghiệm trekking qua đêm")
        );
        toursList.add(tourRepository.save(fansipan));

        // Tour 2: Tà Năng
        Tour taNang = Tour.builder()
                .title("Trekking Tà Năng – Phan Dũng")
                .slug("ta-nang-phan-dung")
                .shortDescription("Cung đường đẹp nhất Việt Nam qua thảo nguyên xanh mướt.")
                .difficulty(DifficultyLevel.MODERATE)
                .durationDays((short) 4).durationNights((short) 3)
                .distanceKm(new BigDecimal("45.0")).maxElevationM(1920)
                .startLocation("Đà Lạt, Lâm Đồng").endLocation("Phan Thiết, Bình Thuận")
                .status(TourStatus.ACTIVE).createdBy(admin.getId()).build();
        mapAttributes(taNang, 
                List.of("Thảo nguyên Tà Năng rộng lớn","Rừng thông cổ thụ","Suối Vàng"),
                List.of("HDV","Lều trại","Bữa ăn","Xe đưa đón điểm cuối"),
                List.of("Bảo hiểm","Chi phí cá nhân"),
                List.of("Sức khoẻ bình thường","Không yêu cầu kinh nghiệm trước")
        );
        toursList.add(tourRepository.save(taNang));

        // Tour 3: Mã Pí Lèng
        Tour mapiLeng = Tour.builder()
                .title("Mã Pí Lèng — Đèo huyền thoại miền đá xám")
                .slug("ma-pi-leng-trek")
                .shortDescription("Chinh phục đèo Mã Pí Lèng và cao nguyên đá Đồng Văn.")
                .difficulty(DifficultyLevel.HARD)
                .durationDays((short) 2).durationNights((short) 1)
                .distanceKm(new BigDecimal("28.0")).maxElevationM(1300)
                .startLocation("Hà Giang").endLocation("Hà Giang")
                .status(TourStatus.ACTIVE).createdBy(admin.getId()).build();
        mapAttributes(mapiLeng, 
                List.of("Đèo Mã Pí Lèng hùng vĩ","Sông Nho Quế xanh biếc","Làng đá cổ Đồng Văn"),
                List.of("HDV địa phương","Lều trại","Bữa ăn"),
                List.of("Di chuyển đến Hà Giang","Bảo hiểm"),
                List.of("Sức khoẻ tốt","Không sợ độ cao")
        );
        toursList.add(tourRepository.save(mapiLeng));

        // Tour 4: Cao Bằng
        Tour caoBang = Tour.builder()
                .title("Khám phá Thác Bản Giốc & Cao Bằng")
                .slug("cao-bang-ban-gioc")
                .shortDescription("Khám phá thác nước biên giới đẹp nhất Việt Nam và các hang động kỳ vĩ.")
                .difficulty(DifficultyLevel.MODERATE)
                .durationDays((short) 3).durationNights((short) 2)
                .distanceKm(new BigDecimal("20.0")).maxElevationM(800)
                .startLocation("Cao Bằng").endLocation("Cao Bằng")
                .status(TourStatus.ACTIVE).createdBy(admin.getId()).build();
        mapAttributes(caoBang,
                List.of("Thác Bản Giốc hùng vĩ","Động Ngườm Ngao kỳ ảo","Hồ Thang Hen thơ mộng"),
                List.of("HDV địa phương","Phí tham quan các điểm","Khách sạn/Homestay","Bữa ăn chính"),
                List.of("Vé máy bay/xe khách đến Cao Bằng","Bảo hiểm du lịch","Nước uống tự mua"),
                List.of("Sức khỏe bình thường","Khả năng đi bộ 4-5 tiếng/ngày")
        );
        toursList.add(tourRepository.save(caoBang));

        // Tour 5: Bạch Mã
        Tour bachMa = Tour.builder()
                .title("Khám phá Vườn Quốc Gia Bạch Mã")
                .slug("bach-ma-national-park")
                .shortDescription("Khám phá hệ sinh thái đa dạng và chinh phục Vọng Hải Đài, Ngũ Hồ.")
                .difficulty(DifficultyLevel.EASY)
                .durationDays((short) 2).durationNights((short) 1)
                .distanceKm(new BigDecimal("15.0")).maxElevationM(1400)
                .startLocation("Huế").endLocation("Huế")
                .status(TourStatus.ACTIVE).createdBy(admin.getId()).build();
        mapAttributes(bachMa,
                List.of("Vọng Hải Đài ngắm toàn cảnh vịnh Lăng Cô","Ngũ Hồ trong vắt mát lạnh","Thác Đỗ Quyên cao 300m"),
                List.of("HDV","Xe đưa đón từ Huế","Khách sạn tại Bạch Mã Summit","Ăn sáng, trưa, tối"),
                List.of("Chi phí cá nhân","Bảo hiểm du lịch"),
                List.of("Sức khỏe cơ bản","Thích hợp cho gia đình và nhóm bạn")
        );
        toursList.add(tourRepository.save(bachMa));

        // Tour 6: Pù Luông
        Tour puLuong = Tour.builder()
                .title("Hành trình Pù Luông xanh mướt")
                .slug("pu-luong-valley")
                .shortDescription("Trekking xuyên qua những bản làng Thái cổ và ruộng bậc thang tầng tầng lớp lớp.")
                .difficulty(DifficultyLevel.EASY)
                .durationDays((short) 2).durationNights((short) 1)
                .distanceKm(new BigDecimal("18.0")).maxElevationM(900)
                .startLocation("Thanh Hóa").endLocation("Thanh Hóa")
                .status(TourStatus.ACTIVE).createdBy(admin.getId()).build();
        mapAttributes(puLuong,
                List.of("Ruộng bậc thang thung lũng Bản Kho Mường","Bản Đôn mộc mạc yên bình","Chợ phiên Phố Đoàn độc đáo"),
                List.of("HDV bản địa","Homestay nhà sàn truyền thống","Bữa ăn ẩm thực Thái","Nước uống chặng đi"),
                List.of("Phương tiện đến Thanh Hóa","Bảo hiểm du lịch","Tiền tip cho HDV"),
                List.of("Sức khỏe dẻo dai","Không yêu cầu kỹ năng leo núi")
        );
        toursList.add(tourRepository.save(puLuong));

        // Tour 7: Chư Yang Sin
        Tour chuYangSin = Tour.builder()
                .title("Chinh phục đỉnh Chư Yang Sin")
                .slug("chu-yang-sin-summit")
                .shortDescription("Thử thách giới hạn bản thân với đỉnh núi cao thứ nhì miền Nam Việt Nam.")
                .difficulty(DifficultyLevel.HARD)
                .durationDays((short) 5).durationNights((short) 4)
                .distanceKm(new BigDecimal("50.0")).maxElevationM(2442)
                .startLocation("Đắk Lắk").endLocation("Đắk Lắk")
                .status(TourStatus.ACTIVE).createdBy(admin.getId()).build();
        mapAttributes(chuYangSin,
                List.of("Chinh phục đỉnh 2442m hoang sơ","Xuyên qua rừng lá kim á nhiệt đới","Hệ thực vật đặc hữu quý hiếm"),
                List.of("HDV chuyên nghiệp & Porter mang đồ chung","Lều trại cao cấp","Bữa ăn giàu năng lượng trên tuyến","Túi ngủ ấm áp"),
                List.of("Di chuyển đến Buôn Ma Thuột","Bảo hiểm du lịch cá nhân","Đồ cá nhân tự mang"),
                List.of("Thể lực xuất sắc, kiểm tra y tế trước tour","Kinh nghiệm trekking ít nhất 2 chuyến dài ngày")
        );
        toursList.add(tourRepository.save(chuYangSin));

        // Tour 8: Lao Thẩn
        Tour laoThan = Tour.builder()
                .title("Săn mây đỉnh Lao Thẩn")
                .slug("lao-than-cloud-hunting")
                .shortDescription("Chinh phục đỉnh Lao Thẩn 2,860m - địa điểm săn mây lý tưởng hàng đầu vùng Tây Bắc.")
                .difficulty(DifficultyLevel.MODERATE)
                .durationDays((short) 2).durationNights((short) 1)
                .distanceKm(new BigDecimal("16.0")).maxElevationM(2860)
                .startLocation("Y Tý, Lào Cai").endLocation("Y Tý, Lào Cai")
                .status(TourStatus.ACTIVE).createdBy(admin.getId()).build();
        mapAttributes(laoThan,
                List.of("Biển mây Y Tý Lao Thẩn","Đón bình minh trên đỉnh 2860m","Hoàng hôn rực rỡ từ lán nghỉ"),
                List.of("HDV & Porter","Lều trại tại điểm lán nghỉ","Bữa ăn chính phong cách Tây Bắc","Nước uống nóng tại lán"),
                List.of("Xe giường nằm Hà Nội - Lào Cai & xe trung chuyển Y Tý","Chi phí cá nhân ngoài chương trình"),
                List.of("Sức khỏe tốt, chịu được không khí lạnh đêm cao nguyên")
        );
        toursList.add(tourRepository.save(laoThan));

        // Tour 9: Nam Cát Tiên
        Tour namCatTien = Tour.builder()
                .title("Thám hiểm Rừng Nam Cát Tiên")
                .slug("nam-cat-tien-expedition")
                .shortDescription("Khám phá rừng mưa nhiệt đới, trekking ngắm thú đêm và Bàu Sấu.")
                .difficulty(DifficultyLevel.EASY)
                .durationDays((short) 3).durationNights((short) 2)
                .distanceKm(new BigDecimal("30.0")).maxElevationM(200)
                .startLocation("Đồng Nai").endLocation("Đồng Nai")
                .status(TourStatus.ACTIVE).createdBy(admin.getId()).build();
        mapAttributes(namCatTien,
                List.of("Bàu Sấu - khu đất ngập nước Ramsar thế giới","Ngắm thú hoang dã đi ăn đêm bằng xe jeep","Hàng cây tùng cổ thụ nghìn năm tuổi"),
                List.of("HDV kiểm lâm","Vé tham quan Bàu Sấu & Rừng Quốc Gia","Lưu trú phòng bungalow/lều","Xe jeep vận chuyển chặng xa"),
                List.of("Chi phí ăn uống tự túc một số bữa","Bảo hiểm du lịch"),
                List.of("Sức khỏe dẻo dai đi bộ đường bằng phẳng","Phù hợp cho mọi lứa tuổi yêu thiên nhiên")
        );
        toursList.add(tourRepository.save(namCatTien));

        log.info("[Seed] Exactly 9 active tours created.");
        return new SeedTours(toursList);
    }

    private void mapAttributes(Tour tour, List<String> highlights, List<String> includes, List<String> excludes, List<String> reqs) {
        List<TourAttribute> attrs = new ArrayList<>();
        if (highlights != null) {
            for (String h : highlights) {
                TourAttribute attr = tourAttributeRepository.findByTypeAndContentIgnoreCase(TourAttributeType.HIGHLIGHT, h.trim())
                        .orElseGet(() -> tourAttributeRepository.save(TourAttribute.builder().type(TourAttributeType.HIGHLIGHT).content(h.trim()).build()));
                attrs.add(attr);
            }
        }
        if (includes != null) {
            for (String i : includes) {
                TourAttribute attr = tourAttributeRepository.findByTypeAndContentIgnoreCase(TourAttributeType.INCLUDE, i.trim())
                        .orElseGet(() -> tourAttributeRepository.save(TourAttribute.builder().type(TourAttributeType.INCLUDE).content(i.trim()).build()));
                attrs.add(attr);
            }
        }
        if (excludes != null) {
            for (String e : excludes) {
                TourAttribute attr = tourAttributeRepository.findByTypeAndContentIgnoreCase(TourAttributeType.EXCLUDE, e.trim())
                        .orElseGet(() -> tourAttributeRepository.save(TourAttribute.builder().type(TourAttributeType.EXCLUDE).content(e.trim()).build()));
                attrs.add(attr);
            }
        }
        if (reqs != null) {
            for (String r : reqs) {
                TourAttribute attr = tourAttributeRepository.findByTypeAndContentIgnoreCase(TourAttributeType.REQUIREMENT, r.trim())
                        .orElseGet(() -> tourAttributeRepository.save(TourAttribute.builder().type(TourAttributeType.REQUIREMENT).content(r.trim()).build()));
                attrs.add(attr);
            }
        }
        tour.setAttributes(attrs);
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

    private void seedTourItineraryAndWaypoints(Tour tour) {
        int D = tour.getDurationDays();
        List<TourWaypoint> waypoints = new ArrayList<>();
        
        // 1. Trailhead (Day 1 start)
        TourWaypoint trailhead = wp(tour, tour.getTitle() + " - Điểm xuất phát", tour.getSlug() + "-start", 1,
                WaypointType.TRAILHEAD, "21.0285", "105.8542", 100, 1, false,
                "Điểm tập kết đoàn và khởi hành", "Kiểm tra danh sách thành viên trước khi đi",
                true, true, true, true,
                WaterSourceType.VILLAGE_TAP, "Nước sạch tại vòi",
                null, null, "115", "Đường bộ cứu hộ", "Bệnh viện đa khoa gần nhất", "5.0", false);
        waypoints.add(trailhead);

        // 2. Intermediate points and campsites
        for (int i = 1; i <= D; i++) {
            TourWaypoint restStop = wp(tour, tour.getTitle() + " - Trạm nghỉ Day " + i, tour.getSlug() + "-rest-" + i, waypoints.size() + 1,
                    WaypointType.REST_STOP, "21.03", "105.86", 200, i, false,
                    "Trạm dừng chân nghỉ ngơi giữa ngày " + i, "Nhắc nhở đoàn bổ sung nước",
                    false, true, true, false,
                    WaterSourceType.STREAM, "Nước suối tự nhiên",
                    null, null, "115", "Đường mòn", "Bệnh viện đa khoa gần nhất", "10.0", false);
            waypoints.add(restStop);

            if (i < D) {
                TourWaypoint campsite = wp(tour, tour.getTitle() + " - Điểm hạ trại Day " + i, tour.getSlug() + "-camp-" + i, waypoints.size() + 1,
                        WaypointType.CAMP_SITE, "21.04", "105.87", 300, i, true,
                        "Điểm cắm trại nghỉ qua đêm ngày " + i, "Hỗ trợ khách dựng lều trước khi trời tối",
                        false, false, false, true,
                        WaterSourceType.STREAM, "Nước suối",
                        AccommodationType.TENT_CAMPING, (short) 15, "115", "Đường mòn", "Bệnh viện đa khoa gần nhất", "15.0", false);
                waypoints.add(campsite);
            }
        }

        // 3. Finish Point (Day D end)
        TourWaypoint finish = wp(tour, tour.getTitle() + " - Điểm kết thúc", tour.getSlug() + "-end", waypoints.size() + 1,
                WaypointType.TRAILHEAD, "21.05", "105.88", 100, D, true,
                "Điểm kết thúc hành trình", "Kiểm tra hành lý và thiết bị thuê trước khi chia tay đoàn",
                true, true, true, false,
                WaterSourceType.VILLAGE_TAP, "Nước sạch tại vòi",
                null, null, "115", "Đường bộ cứu hộ", "Bệnh viện đa khoa gần nhất", "5.0", false);
        waypoints.add(finish);

        int wpIndex = 0;
        for (short d = 1; d <= D; d++) {
            TourWaypoint startWp = waypoints.get(wpIndex);
            TourWaypoint midWp = waypoints.get(wpIndex + 1);
            TourWaypoint endWp = waypoints.get(wpIndex + 2);

            TourDailyItinerary dayIt = itineraryRepository.save(TourDailyItinerary.builder()
                    .tour(tour).dayNumber(d)
                    .dayTitle("Ngày " + d + ": Khám phá " + tour.getTitle())
                    .dayDescription("Hành trình ngày thứ " + d + " của chặng trekking.")
                    .startWaypoint(startWp).endWaypoint(endWp)
                    .overnightWaypoint(d < D ? endWp : null)
                    .distanceKm(tour.getDistanceKm().divide(BigDecimal.valueOf(D), 1, java.math.RoundingMode.HALF_UP))
                    .elevationGainM(100).elevationLossM(100)
                    .walkingHoursMin(new BigDecimal("5.0")).walkingHoursMax(new BigDecimal("7.0"))
                    .dayDifficulty(tour.getDifficulty())
                    .suggestedStartTime(LocalTime.of(8, 0)).suggestedEndTime(LocalTime.of(16, 30))
                    .mealsIncluded(List.of(
                            Map.of("type", "breakfast", "location", "tại trại"),
                            Map.of("type", "lunch",     "location", "trên đường đi"),
                            Map.of("type", "dinner",    "location", "tại trại")))
                    .mealNotes("Bữa ăn tiêu chuẩn do HDV chuẩn bị.")
                    .overnightNotes(d < D ? "Nghỉ ngơi lấy lại sức." : "Kết thúc hành trình.")
                    .safetyNotes("Đi theo nhóm, không tự ý rời đoàn.")
                    .build());

            iw(dayIt, startWp, 1, true, "Xuất phát ngày " + d, "08:00");
            iw(dayIt, midWp, 2, true, "Nghỉ ngơi và dùng bữa trưa", "12:00");
            iw(dayIt, endWp, 3, true, "Kết thúc chặng ngày " + d, "16:30");

            wpIndex += 2;
        }
    }

    private void seedDeparturesBookingsReviewsWeatherAndRentals(SeedUsers users, List<Tour> tours) {
        List<Guide> guides = List.of(users.son(), users.mai(), users.anh());
        List<User> customers = users.customers();

        List<Equipment> activeEquipments = equipmentRepository.findAll().stream()
                .filter(Equipment::getIsActive)
                .collect(java.util.stream.Collectors.toList());

        for (int tourIndex = 0; tourIndex < tours.size(); tourIndex++) {
            Tour tour = tours.get(tourIndex);
            Guide leadGuide = guides.get(tourIndex % guides.size());

            // ─── 1. PAST COMPLETED DEPARTURE (Đầy đoàn 8 khách) ─────────────────────────
            LocalDate pastDepDate = LocalDate.now().minusDays(15 + tourIndex * 2);
            LocalDate pastRetDate = pastDepDate.plusDays(tour.getDurationDays() - 1);
            LocalDate pastCutDate = pastDepDate.minusDays(3);

            TourDeparture pastDep = departureRepository.saveAndFlush(TourDeparture.builder()
                    .tour(tour)
                    .departureDate(pastDepDate)
                    .returnDate(pastRetDate)
                    .cutoffDate(pastCutDate)
                    .pricePerPerson(tour.getDifficulty() == DifficultyLevel.HARD ? new BigDecimal("2500000") : new BigDecimal("1500000"))
                    .maxGroupSize((short) 8)
                    .minGroupSize((short) 2)
                    .bookedSlots((short) 8)
                    .allowJoinTour(true)
                    .meetingPoint(tour.getStartLocation() + " lúc 6:00 AM")
                    .weatherSummary("Thời tiết thuận lợi suốt hành trình")
                    .weatherIcon("sunny")
                    .tempMinC((short) 15)
                    .tempMaxC((short) 25)
                    .weatherUpdatedAt(LocalDateTime.now())
                    .status(DepartureStatus.COMPLETED)
                    .actualStartAt(pastDepDate.atTime(6, 15))
                    .actualEndAt(pastRetDate.atTime(16, 30))
                    .actualParticipants((short) 8)
                    .debriefNotes("Đoàn đi an toàn, sức khỏe tốt, hoàn thành đúng lịch trình.")
                    .build());

            departureGuideRepository.saveAndFlush(DepartureGuide.builder()
                    .id(new DepartureGuideId(pastDep.getId(), leadGuide.getId()))
                    .departure(pastDep)
                    .guide(leadGuide)
                    .role(GuideRoleInTour.LEAD)
                    .confirmedAt(LocalDateTime.now().minusDays(20))
                    .build());

            seedWeatherForDeparture(pastDep, tour);

            for (int bIndex = 0; bIndex < 8; bIndex++) {
                User customerUser = customers.get((tourIndex * 8 + bIndex) % customers.size());

                Booking booking = bookingRepository.save(Booking.builder()
                        .bookingCode("TV-" + pastDepDate.toString().replace("-", "") + "-" + tour.getId().toString().substring(0, 4) + "-" + bIndex)
                        .user(customerUser)
                        .departure(pastDep)
                        .numParticipants((short) 1)
                        .priceSnapshot(pastDep.getPricePerPerson())
                        .subtotalTour(pastDep.getPricePerPerson())
                        .subtotalEquipment(BigDecimal.ZERO)
                        .totalPrice(pastDep.getPricePerPerson())
                        .status(BookingStatus.COMPLETED)
                        .paidAt(pastDepDate.minusDays(10).atTime(10, 0))
                        .bookedAt(pastDepDate.minusDays(10).atTime(10, 0))
                        .build());

                if (!activeEquipments.isEmpty()) {
                    Equipment eq = activeEquipments.get((tourIndex + bIndex) % activeEquipments.size());
                    BigDecimal rentalPrice = eq.getPricePerDay().multiply(BigDecimal.valueOf(tour.getDurationDays()));
                    
                    equipmentRentalRepository.save(EquipmentRental.builder()
                            .booking(booking)
                            .equipment(eq)
                            .quantity((short) 1)
                            .rentalDays((short) tour.getDurationDays())
                            .pricePerDay(eq.getPricePerDay())
                            .subtotal(rentalPrice)
                            .returnedAt(pastRetDate.atTime(16, 30))
                            .returnCondition(EquipmentCondition.GOOD)
                            .damageFee(BigDecimal.ZERO)
                            .notes("Đã hoàn trả trong tình trạng tốt.")
                            .createdAt(booking.getBookedAt())
                            .build());

                    booking.setSubtotalEquipment(rentalPrice);
                    booking.setTotalPrice(booking.getSubtotalTour().add(rentalPrice));
                    bookingRepository.save(booking);
                }

                reviewRepository.save(Review.builder()
                        .user(customerUser)
                        .booking(booking)
                        .tour(tour)
                        .departure(pastDep)
                        .guide(leadGuide)
                        .overallRating((short) (bIndex % 2 == 0 ? 5 : 4))
                        .guideRating((short) 5)
                        .sceneryRating((short) 5)
                        .safetyRating((short) 5)
                        .valueRating((short) (bIndex % 3 == 0 ? 4 : 5))
                        .difficultyRating((short) (tour.getDifficulty() == DifficultyLevel.HARD ? 4 : 3))
                        .title("Trải nghiệm tuyệt vời chặng " + tour.getTitle())
                        .comment("Một chuyến đi tuyệt vời và đáng nhớ cùng TrailViet. Hướng dẫn viên rất nhiệt tình và chu đáo!")
                        .isApproved(true)
                        .build());
            }

            // ─── 2. TWO FUTURE DEPARTURES (Đợt tương lai: trống dưới 10 chỗ - seed 3 chỗ) ────────────────────────────
            LocalDate futDepDate1 = LocalDate.now().plusDays(6);
            LocalDate futRetDate1 = futDepDate1.plusDays(tour.getDurationDays() - 1);
            LocalDate futCutDate1 = futDepDate1.minusDays(3);

            TourDeparture futDep1 = departureRepository.saveAndFlush(TourDeparture.builder()
                    .tour(tour)
                    .departureDate(futDepDate1)
                    .returnDate(futRetDate1)
                    .cutoffDate(futCutDate1)
                    .pricePerPerson(tour.getDifficulty() == DifficultyLevel.HARD ? new BigDecimal("2500000") : new BigDecimal("1500000"))
                    .maxGroupSize((short) 12)
                    .minGroupSize((short) 2)
                    .bookedSlots((short) 3) // Trống 9 chỗ (dưới 10)
                    .allowJoinTour(true)
                    .meetingPoint(tour.getStartLocation() + " lúc 6:30 AM")
                    .weatherSummary("Dự báo nắng ráo, thời tiết đẹp")
                    .weatherIcon("sunny")
                    .tempMinC((short) 16)
                    .tempMaxC((short) 26)
                    .weatherUpdatedAt(LocalDateTime.now())
                    .status(DepartureStatus.OPEN)
                    .build());

            departureGuideRepository.saveAndFlush(DepartureGuide.builder()
                    .id(new DepartureGuideId(futDep1.getId(), leadGuide.getId()))
                    .departure(futDep1)
                    .guide(leadGuide)
                    .role(GuideRoleInTour.LEAD)
                    .confirmedAt(LocalDateTime.now())
                    .build());

            for (int bIndex = 0; bIndex < 3; bIndex++) {
                User customerUser = customers.get((tourIndex * 3 + bIndex) % customers.size());
                bookingRepository.save(Booking.builder()
                        .bookingCode("TV-" + futDepDate1.toString().replace("-", "") + "-" + tour.getId().toString().substring(0, 4) + "-" + bIndex)
                        .user(customerUser)
                        .departure(futDep1)
                        .numParticipants((short) 1)
                        .priceSnapshot(futDep1.getPricePerPerson())
                        .subtotalTour(futDep1.getPricePerPerson())
                        .subtotalEquipment(BigDecimal.ZERO)
                        .totalPrice(futDep1.getPricePerPerson())
                        .status(BookingStatus.CONFIRMED)
                        .paidAt(LocalDateTime.now().minusHours(12))
                        .bookedAt(LocalDateTime.now().minusHours(12))
                        .build());
            }

            LocalDate futDepDate2 = LocalDate.now().plusDays(9);
            LocalDate futRetDate2 = futDepDate2.plusDays(tour.getDurationDays() - 1);
            LocalDate futCutDate2 = futDepDate2.minusDays(3);

            TourDeparture futDep2 = departureRepository.saveAndFlush(TourDeparture.builder()
                    .tour(tour)
                    .departureDate(futDepDate2)
                    .returnDate(futRetDate2)
                    .cutoffDate(futCutDate2)
                    .pricePerPerson(tour.getDifficulty() == DifficultyLevel.HARD ? new BigDecimal("2500000") : new BigDecimal("1500000"))
                    .maxGroupSize((short) 12)
                    .minGroupSize((short) 2)
                    .bookedSlots((short) 3) // Trống 9 chỗ (dưới 10)
                    .allowJoinTour(true)
                    .meetingPoint(tour.getStartLocation() + " lúc 6:30 AM")
                    .weatherSummary("Dự báo nắng ráo, thời tiết đẹp")
                    .weatherIcon("sunny")
                    .tempMinC((short) 16)
                    .tempMaxC((short) 26)
                    .weatherUpdatedAt(LocalDateTime.now())
                    .status(DepartureStatus.OPEN)
                    .build());

            departureGuideRepository.saveAndFlush(DepartureGuide.builder()
                    .id(new DepartureGuideId(futDep2.getId(), leadGuide.getId()))
                    .departure(futDep2)
                    .guide(leadGuide)
                    .role(GuideRoleInTour.LEAD)
                    .confirmedAt(LocalDateTime.now())
                    .build());

            for (int bIndex = 0; bIndex < 3; bIndex++) {
                User customerUser = customers.get((tourIndex * 3 + bIndex + 5) % customers.size());
                bookingRepository.save(Booking.builder()
                        .bookingCode("TV-" + futDepDate2.toString().replace("-", "") + "-" + tour.getId().toString().substring(0, 4) + "-" + bIndex)
                        .user(customerUser)
                        .departure(futDep2)
                        .numParticipants((short) 1)
                        .priceSnapshot(futDep2.getPricePerPerson())
                        .subtotalTour(futDep2.getPricePerPerson())
                        .subtotalEquipment(BigDecimal.ZERO)
                        .totalPrice(futDep2.getPricePerPerson())
                        .status(BookingStatus.CONFIRMED)
                        .paidAt(LocalDateTime.now().minusHours(6))
                        .bookedAt(LocalDateTime.now().minusHours(6))
                        .build());
            }
        }
    }

    private void seedWeatherForDeparture(TourDeparture dep, Tour tour) {
        int duration = tour.getDurationDays();
        for (short d = 1; d <= duration; d++) {
            final short dayNum = d;
            TourDailyItinerary it = itineraryRepository
                    .findByTourIdAndDayNumber(tour.getId(), dayNum).orElse(null);

            weatherDailyRepository.save(DepartureWeatherDaily.builder()
                    .departure(dep)
                    .dayNumber(dayNum)
                    .forecastDate(dep.getDepartureDate().plusDays(dayNum - 1))
                    .itinerary(it)
                    .locationLabel(tour.getTitle() + " - Chặng ngày " + dayNum)
                    .elevationM(it != null && it.getStartWaypoint() != null ? it.getStartWaypoint().getElevationM() : 100)
                    .weatherSummary("Trời nắng ráo, nhiệt độ ôn hòa, tầm nhìn xa tốt.")
                    .weatherIcon("sunny")
                    .tempMinC((short) 16)
                    .tempMaxC((short) 24)
                    .feelsLikeMinC((short) 14)
                    .feelsLikeMaxC((short) 22)
                    .precipitationMm(BigDecimal.ZERO)
                    .precipitationProb((short) 5)
                    .windSpeedKmh((short) 12)
                    .windGustKmh((short) 18)
                    .humidityPct((short) 65)
                    .visibilityKm(new BigDecimal("12.0"))
                    .uvIndex((short) 6)
                    .warningLevel(WarningLevel.INFO)
                    .dataSource("openweathermap")
                    .build());
        }
    }

    private User createUser(String email, String phone, String pw, boolean isAdmin) {
        return userRepository.saveAndFlush(User.builder()
                .email(email).phone(phone).passwordHash(pw)
                .isVerified(true).isActive(true).isAdmin(isAdmin).build());
    }

    private void createCustomer(User user, String fullName, String dob, FitnessLevel fitness) {
        customerRepository.saveAndFlush(Customer.builder()
                .user(user).fullName(fullName)
                .dateOfBirth(LocalDate.parse(dob))
                .nationality("Việt Nam").fitnessLevel(fitness).build());
    }

    private Guide createGuide(User user, String displayName, String bio, short expYears,
                               List<Map<String, Object>> certs,
                               List<String> langs, List<String> specs,
                               String province, String rating, int totalReviews, int toursLed) {
        return guideRepository.saveAndFlush(Guide.builder()
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

    private void updateSeededTourImages() {
        // Không cần thiết vì ảnh đã được seed đầy đủ trong seedTourImages
    }

    private void ensureFutureDeparturesForTours() {
        log.info("[DataInitializer] Đang kiểm tra để đảm bảo mỗi tour có ít nhất 2 đợt khởi hành trong tương lai...");
        List<Guide> guides = guideRepository.findAll();
        if (guides.isEmpty()) {
            log.warn("[DataInitializer] Không tìm thấy HDV nào trong database. Bỏ qua bổ sung ngày khởi hành.");
            return;
        }

        List<Tour> tours = tourRepository.findAll();
        List<TourDeparture> allDeps = departureRepository.findAll();
        LocalDate today = LocalDate.now();

        for (Tour tour : tours) {
            long upcomingCount = allDeps.stream()
                    .filter(d -> d.getTour().getId().equals(tour.getId()))
                    .filter(d -> d.getDepartureDate() != null && d.getDepartureDate().isAfter(today))
                    .filter(d -> d.getStatus() == DepartureStatus.OPEN || d.getStatus() == DepartureStatus.SCHEDULED)
                    .count();

            if (upcomingCount < 2) {
                long needed = 2 - upcomingCount;
                log.info("[DataInitializer] Tour '{}' ({}) chỉ có {} đợt khởi hành tương lai. Tiến hành bổ sung {} đợt...",
                        tour.getTitle(), tour.getId(), upcomingCount, needed);

                List<TourDeparture> tourDeps = allDeps.stream()
                        .filter(d -> d.getTour().getId().equals(tour.getId()))
                        .collect(java.util.stream.Collectors.toList());

                BigDecimal price = new BigDecimal("2000000");
                String meetingPoint = tour.getStartLocation() != null ? tour.getStartLocation() : "Điểm tập kết mặc định";
                short maxGroup = 15;
                short minGroup = 2;
                boolean allowJoin = true;
                String weatherSummary = "Nắng đẹp, thời tiết tốt";
                String weatherIcon = "sunny";
                short tempMin = 15;
                short tempMax = 25;

                if (!tourDeps.isEmpty()) {
                    TourDeparture sample = tourDeps.get(0);
                    if (sample.getPricePerPerson() != null) price = sample.getPricePerPerson();
                    if (sample.getMeetingPoint() != null) meetingPoint = sample.getMeetingPoint();
                    if (sample.getMaxGroupSize() != null) maxGroup = sample.getMaxGroupSize();
                    if (sample.getMinGroupSize() != null) minGroup = sample.getMinGroupSize();
                    allowJoin = sample.getAllowJoinTour() != null ? sample.getAllowJoinTour() : true;
                    if (sample.getWeatherSummary() != null) weatherSummary = sample.getWeatherSummary();
                    if (sample.getWeatherIcon() != null) weatherIcon = sample.getWeatherIcon();
                    if (sample.getTempMinC() != null) tempMin = sample.getTempMinC();
                    if (sample.getTempMaxC() != null) tempMax = sample.getTempMaxC();
                }

                Guide leadGuide = guides.get(0);
                if (!tourDeps.isEmpty()) {
                    for (TourDeparture td : tourDeps) {
                        if (td.getGuideAssignments() != null && !td.getGuideAssignments().isEmpty()) {
                            leadGuide = td.getGuideAssignments().get(0).getGuide();
                            break;
                        }
                    }
                }

                int seeded = 0;
                int daysOffset = 10;
                while (seeded < needed && daysOffset < 150) {
                    LocalDate candidateDate = today.plusDays(daysOffset);
                    
                    boolean dateExists = departureRepository.findByTourIdAndDepartureDate(tour.getId(), candidateDate).isPresent();
                    if (!dateExists) {
                        int duration = tour.getDurationDays() != null && tour.getDurationDays() > 0 ? tour.getDurationDays() : 1;
                        LocalDate returnDate = candidateDate.plusDays(duration - 1);
                        LocalDate cutoffDate = candidateDate.minusDays(2);

                        TourDeparture newDep = departureRepository.saveAndFlush(TourDeparture.builder()
                                .tour(tour)
                                .departureDate(candidateDate)
                                .returnDate(returnDate)
                                .cutoffDate(cutoffDate)
                                .pricePerPerson(price)
                                .maxGroupSize(maxGroup)
                                .minGroupSize(minGroup)
                                .bookedSlots((short) 0)
                                .allowJoinTour(allowJoin)
                                .meetingPoint(meetingPoint)
                                .weatherSummary(weatherSummary)
                                .weatherIcon(weatherIcon)
                                .tempMinC(tempMin)
                                .tempMaxC(tempMax)
                                .weatherUpdatedAt(LocalDateTime.now())
                                .status(DepartureStatus.OPEN)
                                .build());

                        departureGuideRepository.saveAndFlush(DepartureGuide.builder()
                                .id(new DepartureGuideId(newDep.getId(), leadGuide.getId()))
                                .departure(newDep)
                                .guide(leadGuide)
                                .role(GuideRoleInTour.LEAD)
                                .confirmedAt(LocalDateTime.now())
                                .build());

                        log.info("[DataInitializer] Đã seed đợt khởi hành mới cho tour '{}': ngày {}", tour.getTitle(), candidateDate);
                        seeded++;
                    }
                    daysOffset += 10;
                }
            }
        }
    }

    private void updateTourRatingAndReviewsStats() {
        log.info("[DataInitializer] Đang cập nhật rating và reviews cho toàn bộ tour...");
        List<Tour> tours = tourRepository.findAll();
        for (Tour tour : tours) {
            Double avgRating = reviewRepository.avgRatingByTour(tour.getId());
            long totalReviews = reviewRepository.countByTourIdAndIsApproved(tour.getId(), true);
            tour.setAvgRating(avgRating != null ? BigDecimal.valueOf(avgRating) : BigDecimal.ZERO);
            tour.setTotalReviews((int) totalReviews);
            tourRepository.save(tour);
            log.info("[DataInitializer] Cập nhật tour '{}': avgRating={}, totalReviews={}", tour.getTitle(), tour.getAvgRating(), tour.getTotalReviews());
        }
    }
}


