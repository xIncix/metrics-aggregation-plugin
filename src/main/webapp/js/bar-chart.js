/* global jQuery, view, echarts */
(function ($) {

    function initBarChart(root) {
        const $root = root ? $(root) : $(document);

        $root.find('.bar-chart').each(function () {
            const $el = $(this);
            const metricId = $el.data('metricId');
            const domElement = this;

            // optional: data-last-n="5" im HTML setzen
            const lastN = Number($el.data('lastN')) || 5;

            if (!metricId || typeof view === "undefined") {
                return;
            }

            view.getNameAndValuesForLastBuilds(metricId, lastN, function (res) {
                const data = res.responseJSON;
                if (!data || !Array.isArray(data.values)) {
                    return;
                }

                renderBarChart(domElement, data.values, data.name, data.isPercentage);
            });
        });
    }

    function renderBarChart(domElement, points, metricName, isPercentage) {
        if (!domElement) {
            return;
        }

        const chartDom = domElement instanceof jQuery ? domElement[0] : domElement;
        const myChart = echarts.init(chartDom);

        const labels = points.map(p => `#${p.build}`);
        const values = points.map(p => (p.value == null ? null : p.value));

        const option = {
            tooltip: {
                trigger: 'axis',
                valueFormatter: (v) => {
                    if (v == null) return 'no data';
                    return isPercentage ? `${Number(v).toFixed(2)}%` : `${v}`;
                }
            },
            grid: { left: 40, right: 20, top: 20, bottom: 40 },
            xAxis: {
                type: 'category',
                data: labels,
                axisLabel: { rotate: 0 },
                name: metricName,
                nameLocation: 'middle',
                nameGap: 30
            },
            yAxis: {
                type: 'value',
                axisLabel: {
                    formatter: (v) => isPercentage ? `${v}%` : `${v}`
                },
                min: isPercentage ? 0 : null,
                max: isPercentage ? 100 : null
            },
            series: [{
                name: metricName,
                type: 'bar',
                data: values,
                label: {
                    show: true,
                    position: 'top',
                    formatter: (p) => {
                        if (p.value == null) return '';
                        return isPercentage ? `${p.value.toFixed(1)}%` : `${p.value}`;
                    }
                }
            }]
        };

        myChart.setOption(option);
    }

    $(document).ready(function () {
        initBarChart();
    });

    window.initBarChart = initBarChart;

})(jQuery);
