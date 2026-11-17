USE inventory;

DROP PROCEDURE IF EXISTS GetMonthlySalesForEachStore;
DROP PROCEDURE IF EXISTS GetAggregateSalesForCompany;
DROP PROCEDURE IF EXISTS GetTopSellingProductsByCategory;
DROP PROCEDURE IF EXISTS GetTopSellingProductByStore;

DELIMITER //

-- Monthly sales of every store for a year and month, highest first
CREATE PROCEDURE GetMonthlySalesForEachStore(IN year_param INT, IN month_param INT)
BEGIN
    SELECT od.store_id,
           SUM(od.total_price) AS total_sales,
           MONTH(od.date)      AS sale_month,
           YEAR(od.date)       AS sale_year
    FROM order_details od
    WHERE YEAR(od.date) = year_param
      AND MONTH(od.date) = month_param
    GROUP BY od.store_id, MONTH(od.date), YEAR(od.date)
    ORDER BY total_sales DESC;
END //

-- Total sales of the whole company for a year and month
CREATE PROCEDURE GetAggregateSalesForCompany(IN year_param INT, IN month_param INT)
BEGIN
    SELECT SUM(od.total_price) AS total_sales,
           MONTH(od.date)      AS sale_month,
           YEAR(od.date)       AS sale_year
    FROM order_details od
    WHERE YEAR(od.date) = year_param
      AND MONTH(od.date) = month_param
    GROUP BY MONTH(od.date), YEAR(od.date);
END //

-- Best selling product(s) of every category for a month and year (ties are all returned)
CREATE PROCEDURE GetTopSellingProductsByCategory(IN target_month INT, IN target_year INT)
BEGIN
    SELECT p.category,
           p.name,
           SUM(oi.quantity)              AS total_quantity_sold,
           SUM(oi.quantity * oi.price)   AS total_sales
    FROM order_item oi
    JOIN order_details od ON od.id = oi.order_id
    JOIN product p        ON p.id = oi.product_id
    WHERE MONTH(od.date) = target_month
      AND YEAR(od.date) = target_year
    GROUP BY p.category, p.name
    HAVING SUM(oi.quantity) = (
        SELECT MAX(t.qty)
        FROM (
            SELECT p2.category AS category, p2.name AS name, SUM(oi2.quantity) AS qty
            FROM order_item oi2
            JOIN order_details od2 ON od2.id = oi2.order_id
            JOIN product p2        ON p2.id = oi2.product_id
            WHERE MONTH(od2.date) = target_month
              AND YEAR(od2.date) = target_year
            GROUP BY p2.category, p2.name
        ) t
        WHERE t.category = p.category
    )
    ORDER BY p.category, p.name;
END //

-- Best selling product(s) of every store for a month and year (ties are all returned)
CREATE PROCEDURE GetTopSellingProductByStore(IN target_month INT, IN target_year INT)
BEGIN
    SELECT p.name                        AS product_name,
           od.store_id                   AS store_id,
           SUM(oi.quantity)              AS total_quantity_sold,
           SUM(oi.quantity * oi.price)   AS total_sales
    FROM order_item oi
    JOIN order_details od ON od.id = oi.order_id
    JOIN product p        ON p.id = oi.product_id
    WHERE MONTH(od.date) = target_month
      AND YEAR(od.date) = target_year
    GROUP BY od.store_id, p.name
    HAVING SUM(oi.quantity) = (
        SELECT MAX(t.qty)
        FROM (
            SELECT od2.store_id AS store_id, p2.name AS name, SUM(oi2.quantity) AS qty
            FROM order_item oi2
            JOIN order_details od2 ON od2.id = oi2.order_id
            JOIN product p2        ON p2.id = oi2.product_id
            WHERE MONTH(od2.date) = target_month
              AND YEAR(od2.date) = target_year
            GROUP BY od2.store_id, p2.name
        ) t
        WHERE t.store_id = od.store_id
    )
    ORDER BY od.store_id, p.name;
END //

DELIMITER ;
