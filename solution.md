A new class ECustomerSegment was created to represent information about customer segments.
This enum contains constants, each of which represents a customer segment and its associated credit modifier.
This allows you to more explicitly define the possible values of customer segments and their associated credit modifiers.

Previously DecisionEngineConstants class contained only static fields representing constants.
Now the class contains only static fields and methods to access those fields.
This allows constants to be accessed via methods, which can be useful for access control or value return logic.

The most important shortcoming of TICKET-101 was that the service was not returning
new suitable period for a loan.
Now if your possible loan is less than minimal loan amount then we return the suitable month.
Else we return maximum loan amount.