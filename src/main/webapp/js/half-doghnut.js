/* global jQuery, view, echarts */
(function ($) {
    $(document).ready(function () {
        const el = $('#halfDoughnut');
        if (!el.length || typeof view === "undefined") return;

        const metricId = el.data('metricId');

        if (view.getNameAndValueForNumberValues) {
            view.getNameAndValueForNumberValues(metricId, function (res) {
                const data = res.responseJSON;

                if (!data || typeof data.value === "undefined") {
                    console.warn("⚠️ Keine gültigen Daten erhalten:", res);
                    return;
                }

                renderHalfDoghnutChart(data.value, data.name);
            });
        }

        function renderHalfDoghnutChart(value, metricName) {
            const chartDom = document.getElementById('halfDoughnut');
            if (!chartDom) return;

            const myChart = echarts.init(chartDom);

            const option = {
                tooltip: { trigger: 'item' },
                series: [{
                    name: metricName,
                    type: 'pie',
                    radius: ['40%', '70%'],
                    center: ['50%', '70%'],
                    startAngle: 180,
                    endAngle: 360,
                    avoidLabelOverlap: false,
                    label: {
                        show: true,
                        position: 'center',
                        formatter: `${metricName}\n${value}`,
                        fontSize: 18,
                        fontWeight: 'bold'
                    },
                    data: [
                        { value: value, name: metricName },
                        { value: 0, name: '' } // Leerwert für Halbkreis
                    ]
                }]
            };

            myChart.setOption(option);
        }
    });
})(jQuery);
