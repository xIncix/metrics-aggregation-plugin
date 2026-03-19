/* global jQuery, view, echarts */
(function ($) {

    function initTreeMap(root) {
        const $root = root ? $(root) : $(document);

        $root.find('.tree-map').each(function () {
            const $el = $(this);
            const metricId = $el.data('metricId');
            const domElement = this;

            if (!metricId || typeof view === "undefined") {
                return;
            }

            view.getDisplayNameOfMetric(metricId, function (resName) {
                const displayName = (resName && resName.responseJSON) ? resName.responseJSON : metricId;

                view.getPackageClassHierarchy(metricId, function (resData) {
                    const data = resData && resData.responseJSON ? resData.responseJSON : null;
                    if (!Array.isArray(data)) {
                        return;
                    }

                    renderTreeMap(domElement, data, metricId, displayName);
                });
            });
        });
    }

    function renderTreeMap(domElement, packageNodes, metricId, displayName) {
        const chartDom = domElement instanceof jQuery ? domElement[0] : domElement;
        const myChart = echarts.init(chartDom);

        const option = {
            tooltip: {
                trigger: 'item',
                formatter: function (info) {
                    const node = info && info.data ? info.data : {};
                    const name = node.name ? String(node.name) : '';

                    const isLeaf = !(Array.isArray(node.children) && node.children.length > 0);

                    if (!isLeaf) {
                        return name;
                    }

                    const v = info.value;
                    const valueText = (v == null || !Number.isFinite(Number(v)))
                        ? 'no data'
                        : Number(v);

                    return `${name}<br/>${displayName}: ${valueText}`;
                }
            },
            series: [{
                type: 'treemap',
                name: displayName,
                data: packageNodes,

                nodeClick: 'zoomToNode',
                roam: false,

                label: { show: true, formatter: '{b}' },
                upperLabel: { show: true, height: 24 },

                itemStyle: {
                    borderColor: '#fff',
                    borderWidth: 1,
                    gapWidth: 2
                }
            }]
        };

        myChart.setOption(option);

        window.addEventListener('resize', function () {
            try { myChart.resize(); } catch (e) { /* ignore */ }
        });
    }

    $(document).ready(function () { initTreeMap(); });
    window.initTreeMap = initTreeMap;

})(jQuery);
