library(tidyverse)
library(tidyjson)
library(boot)

## Note: do not forget to "Set working directory" -> "To source file location"

log_breaker <- scales::trans_breaks('log10', function(x) 10^x, n = 6)
log_labeller <- scales::trans_format('log10', function(x) parse(text = ifelse(x == 0, '1', ifelse(x == 1, '10', paste0('10^', x)))))
compute_ci <- function(data, f = mean, R = 1000, conf = 0.95) {
    data.boot <- boot(data, function(d,ix) mean(d[ix]), R)
    boot.ci(data.boot, conf = conf, type = "norm")$normal[2:3] %>%
        setNames(c("time.mean.ci.low", "time.mean.ci.high")) %>%
        list()
}

filename_results <- "../build/reports/jmh/results.json"
raw <- read_json(filename_results)
data_all <- raw %>% 
    gather_array() %>% 
    spread_values(
        method = jstring("benchmark"),
        n = jstring("params", "n"), 
        k = jstring("params", "k")
    ) %>% 
    mutate(
        n = as.numeric(n),
        k = as.numeric(k)
    ) %>%
    enter_object("primaryMetric", "rawData") %>% 
    gather_array() %>% 
    gather_array() %>% 
    append_values_number("time") %>% 
    as_tibble() %>% 
    select(-document.id, -starts_with("array.index")) %>% 
    mutate(method = str_remove(method, ".*\\.")) %>% 
    mutate(method = fct_relevel(method, "getModel", "getValue"))
data_agg <- data_all %>% 
    group_by(method, n, k) %>% 
    summarize(
        time.mean = mean(time),
        time.median = median(time),
        time.sd = sd(time),
        time.mad = mad(time),
        time.mean.ci = compute_ci(time, mean, conf = 0.99)
    ) %>% 
    ungroup() %>%
    unnest_wider(time.mean.ci)
data_agg

data_getModel <- data_agg %>% 
    filter(method == "getModel") %>% 
    select(-k)
data_getValue <- data_agg %>% 
    filter(method == "getValue")
data_merged <- data_getValue %>% 
    bind_rows(data_getModel %>% mutate(k = 1)) %>%
    bind_rows(data_getModel %>% mutate(k = n))

data_merged %>% 
    mutate(n_label = str_c("N = ", format(n, scientific = TRUE))) %>% 
    ggplot(aes(x = k, y = time.mean, group = method)) +
    facet_wrap(vars(n_label)) +
    geom_ribbon(
        aes(ymin = pmax(1, time.mean.ci.low), 
            ymax = time.mean.ci.high,
            fill = method),
        alpha = 0.5, show.legend = F
    ) +
    geom_line(
        aes(color = method),
        size = 1
    ) +
    scale_x_log10(breaks = log_breaker, labels = log_labeller) +
    scale_y_log10(breaks = log_breaker, labels = log_labeller) +
    theme_bw() +
    labs(
        title = "Benchmark: getModel vs getValue",
        x = "k",
        y = "Time, us",
        color = "Method"
    )
ggsave("plot_bench_getModel_vs_getValue.png", dpi = 300, width = 8, height = 4)
