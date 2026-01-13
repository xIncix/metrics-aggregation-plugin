/* global jQuery, view, echarts */
(function ($) {
    $(document).ready(function () {

        $('.half-doughnut').each(function () {
            const $el = $(this);
            const metricId = $el.data('metricId');
            const domElement = this;

            if (!metricId || typeof view === "undefined") return;

            view.isPercentageMetric(metricId, function (res) {
                const isPercentage = res.responseJSON === true;

                if (isPercentage) {
                    view.getNameAndValueForPercentageValues(metricId, function (res2) {
                        const data = res2.responseJSON;
                        if (!data || typeof data.value === "undefined") return;

                        renderHalfDoughnutChart(domElement, data.value, data.name, true);
                    });
                }
                else {
                    view.getNameAndValueForIntValues(metricId, function (res2) {
                        const data = res2.responseJSON;
                        if (!data || typeof data.value === "undefined") return;

                        renderHalfDoughnutChart(domElement, data.value, data.name, false);
                    });
                }
            });
        });

        function renderHalfDoughnutChart(domElement, value, metricName, isPercentage) {
            const chartDom = domElement instanceof jQuery ? domElement[0] : domElement;
            if (!chartDom) return;

            const myChart = echarts.init(chartDom);

            const labelText = `${metricName}\n${isPercentage ? value.toFixed(2) + '%' : value}`;

            const option = {
                tooltip: { trigger: 'item' },
                series: [{
                    name: metricName,
                    type: 'pie',
                    radius: ['50%', '70%'],
                    center: ['50%', '70%'],
                    startAngle: 180,
                    endAngle: 360,
                    avoidLabelOverlap: false,
                    label: {
                        show: true,
                        position: 'center',
                        formatter: labelText,
                        fontSize: 15,
                        fontWeight: 'bold'
                    },
                    color: ['green', 'red'],
                    data: [
                        { value: value, name: metricName },
                        { value: isPercentage ? (100 - value) : 0, name: '' }
                    ]
                }]
            };

            myChart.setOption(option);
        }
    });
})(jQuery);
