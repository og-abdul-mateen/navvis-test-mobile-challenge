# NavVis Mobile Code Challenge (v11)  

## Overview 
The task is to write a mobile app that streams data from a service. The data must be processed, parsed, and displayed in a table view.

## Networking
The REST service allows to stream values from a server via two endpoints:

1. **numbers endpoint**: `https://navvis.com/numbers.json`
2. **inputs endpoint**: `https://navvis.com/inputs.json`

* Both endpoints return values in an array.
* Each input at index `i` corresponds to the number at index `i` of the stream.
* The endpoints operate independently of each other and return values available at the time of each request, but might also truncate the response to a maximum count of values in the array. 
* The UI should display results incrementally.
* If no more values are available, the returned array is empty and fetching can be considered done.
* If the server cannot deliver a specific, individual value, it will be represented with `null`.
* The service is still in development, so you need to mock the endpoints.
* Provide a mechanism to easily switch between mock and real implementation.

### Numbers Endpoint Response
* The range of the number values is expected to be 0...255.
* A simple example:

```json
{
  "numbers": [4, 150, 12, 21, 136, 16]
}
```

### Inputs Endpoint Response
* The input values are the result of a complex processing on the server.
* The input values are expected to be in the range of a 16bit unsigned integer.

A simple example:
```json
{
  "computation_input": [10, 25, 17, 39, 1, 76, 4123]
}
```

## Number Parsing 
The numbers, i.e., bytes in the array shall be parsed and each number shall be converted as follows: 

* The two least significant bits define the "section index" 
* The most significant bit defines the "result bit" 
* The intermediate bits determine the "item value" 
* The "section index" must be matched against 4 section titles:
  * 0 -> SECTION 1
  * 1 -> SECTION 2 
  * 2 -> SECTION 3 
  * 3 -> SECTION 4  
* The "item value" must be matched against 6 titles: 
  * 0 -> Item 1 
  * 1 -> Item 2 
  * 2 -> Item 3 
  * 3 -> Item 4 
  * 4 -> Item 5 
  * 5 -> Item 6 
* The "result" bit acts as a flag for displaying a computation result: 
  * 0 -> computation result not needed, do not show it
  * 1 -> show computation result
 
### Input Value Processing
* The input value serves as input for computing a result that shall be shown to the user. Assume it can take up to a few milliseconds of heavy CPU computation to obtain the result, including some outliers.
* The actual algorithm cannot be shared with you, so just return the last two digits instead.

## Display & Layout 
* Each valid number carries important information and shall be shown as an entry in a list.
* Any error during the processing of the input values shall be visualized.
* Items that belong to the same section shall be grouped together under their section title.
* If the result bit is set, show the result of the computation.
* The section title entries must be sorted according to their value.
* The Item entries must be sorted according to their value and computation result.

Example: 

```
-----------------------
SECTION 1
-----------------------
Item 4                 
-----------------------
Item 4        result=03    
-----------------------
Item 4        result=12
-----------------------
Item 5
-----------------------
SECTION 2
-----------------------
Item 2        result=01
-----------------------
```
 
A fancy UI is not part of the challenge, a simple list with basic entries will suffice.

## Requirements 
* iOS
  * Implemented in Swift/SwiftUI. Use Xcode 26.x, modern language features and Apple frameworks.
  * No 3rd-party frameworks. Swift packages from Apple are allowed. 
  * Full Xcode project, ready to run. 
* Android
  * Implemented in Kotlin/Compose. 
  * Fully working project, ready to run.
  

## Expectations 
* Clean, maintainable and readable code that separates concerns.
* Provide evidence that your solution works correctly.
* Ensure your solution is resilient against real-world server errors.
* Demonstrate that your solution is performant.
* Any requirement that is undefined or ambiguous is intentional. Make reasonable assumptions, document them, and justify the approach you chose.
* When using AI, document how and where you used it.

## Final notes 
* Perfection is not a requirement, so please do not spend more than one day on the task. 
* We don't expect a production-grade app. Prioritize what matters most and document where you took shortcuts for what reason and how you would fill the missing gaps.
* Expect a human to review your code.
* If you complete this challenge successfully, we will discuss your solution in a technical interview, among other things.
* Good luck and have fun!
