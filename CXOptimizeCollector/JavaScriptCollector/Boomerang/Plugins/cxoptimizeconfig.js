/*
 * Copyright 2014 - 2018 Cognizant Technology Solutions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

BOOMR.t_end = new Date().getTime();
BOOMR.init({
	beacon_url: 'http://cxoptimizehost/cxoptimize/api/insertBoomerangStats',
	beacon_type: 'POST',
	instrument_xhr: false,
	CXOptimize: {
		beacon_url: 'http://cxoptimizehost/cxoptimize/api/insertBoomerangStats',
		client: 'Cognizant',
		project: 'ILPB',
		scenario: 'RUM',
		build: '2.0.0',
		release: '2.0.0',
		application: 'ILPB',
		includeResourceTiming: true,
		includeDOM: true,
		includeDOMCount: true,
		trackXHR: false,
		runID: '0',
		licenseKey:'BuW353Avp1ubdN+VGq434D9S/igG8GVTxLadzSArLEi+k4fU9jxmsimvIrYvDrcd',
		apiToken:'cGFjZXVzZXI6cXdlcnR5dWlvcA=='
	}
});