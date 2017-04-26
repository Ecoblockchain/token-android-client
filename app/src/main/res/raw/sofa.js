var SOFA = {
  initialize: function() {
    setTimeout(function(){ console.log(SOFAHost.getAccounts()); }, 1000);
    setTimeout(function(){ console.log(SOFAHost.approveTransaction("<transaction data json>")); }, 2000);
    setTimeout(function(){ console.log(SOFAHost.signTransaction("<transaction data json>")); }, 3000);

    return true;
  }
}