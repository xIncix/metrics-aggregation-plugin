(function () {
    document.addEventListener('DOMContentLoaded', function () {
        let editMode = false;

        const grid = new Muuri('.dashboard-grid', {
            dragEnabled: true,

            dragStartPredicate: function (item, event) {
                if (!editMode) {
                    return false;
                }
                return !event.target.closest('.delete-widget-btn');
            },

            layout: {
                fillGaps: true,
                rounding: true
            }
        });

        const configureBtn = document.getElementById('configure-dashboard-btn');
        const saveBtn = document.getElementById('save-dashboard-btn');
        const addWidgetBtn = document.getElementById('add-widget-btn');

        if (!configureBtn || !saveBtn || !addWidgetBtn) {
            console.error('Dashboard buttons not found');
            return;
        }

        configureBtn.addEventListener('click', function () {
            editMode = true;
            document.body.classList.add('dashboard-edit-mode');

            configureBtn.style.display = 'none';
            saveBtn.style.display = 'inline-flex';
            addWidgetBtn.style.display = 'inline-flex';

            grid.refreshItems().layout();
        });

        saveBtn.addEventListener('click', function () {
            editMode = false;
            document.body.classList.remove('dashboard-edit-mode');

            saveBtn.style.display = 'none';
            addWidgetBtn.style.display = 'none';
            configureBtn.style.display = 'inline-flex';

            grid.refreshItems().layout();
        });

        const gridElement = document.querySelector('.dashboard-grid');

        gridElement.addEventListener('click', function (e) {
            const deleteBtn = e.target.closest('.delete-widget-btn');
            if (!deleteBtn) {
                return;
            }

            e.preventDefault();
            e.stopPropagation();

            const itemElem = deleteBtn.closest('.item');
            const item = grid.getItem(itemElem);
            if (!item) {
                return;
            }

            grid.remove([item], {removeElements: true});
        });

        addWidgetBtn.addEventListener('click', showAddWidgetForm);

        function showAddWidgetForm() {
            const template = document.getElementById('add-widget-form');
            if (!template) {
                console.error('Add widget form template not found');
                return;
            }

            const form = template.firstElementChild.cloneNode(true);
            const title = template.dataset.title;

            const chartDropdown = form.querySelector('[data-name="chartType"]');
            const metricDropdown = form.querySelector('[data-name="metric"]');

            initDropdown(chartDropdown, [
                {label: 'Half Doughnut', value: 'half-doughnut'},
                {label: 'Bar Chart', value: 'bar'},
                {label: 'Line Chart', value: 'line'}
            ]);

            let metricLabelAndId = document.getElementById('metric-label-id');
            metricLabelAndId = JSON.parse(metricLabelAndId.dataset.metricLabelId);

            initDropdown(metricDropdown, metricLabelAndId.map(m => ({
                label: m.label,
                value: m.id
            })));

            dialog.form(form, {
                title: title,
                okText: 'Add',
                cancelText: 'Cancel',
                submitButton: false
            }).then(() => {
                const chartType = chartDropdown.dataset.value;
                const metric = metricDropdown.dataset.value;

                if (!chartType || !metric) {
                    console.warn('Chart type or metric missing');
                    return;
                }

                console.log('Add widget:', {chartType, metric});
                createWidget(chartType, metric);
            });
        }

        function initDropdown(dropdown, options) {
            if (!dropdown) {
                return;
            }

            const button = dropdown.querySelector('.jenkins-dropdown__button');
            const label = dropdown.querySelector('.jenkins-dropdown__label');
            const menu = dropdown.querySelector('.jenkins-dropdown__menu');

            options.forEach(opt => {
                const li = document.createElement('li');
                li.textContent = opt.label;
                li.dataset.value = opt.value;

                li.addEventListener('click', () => {
                    label.textContent = opt.label;
                    dropdown.dataset.value = opt.value;
                    dropdown.classList.remove('is-open');
                });

                menu.appendChild(li);
            });

            button.addEventListener('click', function (e) {
                e.preventDefault();
                dropdown.classList.toggle('is-open');
            });

            document.addEventListener('click', function (e) {
                if (!dropdown.contains(e.target)) {
                    dropdown.classList.remove('is-open');
                }
            });
        }

        function createWidget(chartType, metric) {
            const itemElem = document.createElement('div');
            itemElem.className = 'item';

            const content = document.createElement('div');
            content.className = 'item-content';

            const deleteBtn = document.createElement('button');
            deleteBtn.className = 'delete-widget-btn jenkins-button jenkins-button--icon';

            deleteBtn.textContent = 'X'; //TODO Fix this

            content.appendChild(deleteBtn);

            const chartContainer = document.createElement('div');
            chartContainer.className = 'chart-container';

            content.appendChild(chartContainer);
            itemElem.appendChild(content);

            renderChart(chartType, metric, chartContainer);

            grid.add(itemElem);
            grid.refreshItems().layout();
        }

        function renderChart(chartType, metric, container) {
            switch (chartType) {
                case 'half-doughnut':
                    renderHalfDoughnut(metric, container);
                    break;

                case 'bar':
                    console.warn('Bar chart not implemented yet');
                    break;

                case 'line':
                    console.warn('Line chart not implemented yet');
                    break;

                default:
                    console.error('Unknown chart type:', chartType);
            }
        }

        function renderHalfDoughnut(metric, container) {
            const el = document.createElement('div');
            el.className = 'half-doughnut';
            el.dataset.metricId = metric;

            container.appendChild(el);

            if (window.initHalfDoughnuts) {
                window.initHalfDoughnuts(container);
            }
        }

        setTimeout(function () {
            grid.refreshItems().layout();
        }, 300);
    });
})();
